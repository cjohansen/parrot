(ns parrot.core
  (:require [clojure.test :as test]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn comparable? [a b]
  (cond
    (and (string? a) (instance? java.util.regex.Pattern b))
    (re-find b a)

    (and (string? b) (instance? java.util.regex.Pattern a))
    (re-find a b)

    :default
    (= a b)))

(defmulti match? (fn [k spec req] k))

(defn normalize-keys [m]
  (->> (keys m)
       (map (fn [k] [(keyword (str/lower-case k)) (get m k)]))
       (into {})))

(defmethod match? :headers [k spec req]
  (let [req-headers (normalize-keys (req k))
        spec-headers (normalize-keys (spec k))]
    (every? #(comparable? (get spec-headers %)
                          (get req-headers %))
            (keys spec-headers))))

(defmethod match? :default [k spec req]
  (comparable? (k spec) (k req)))

(defn normalize-spec [s]
  (if (vector? s)
    {:method (first s)
     :url (second s)}
    s))

(defn matches-request? [req spec]
  (let [spec (normalize-spec spec)]
    (loop [ks (keys spec)]
      (if (nil? ks)
        true
        (let [k (first ks)]
          (if (match? k spec req)
            (recur (next ks))
            false))))))

(defn find-suitable-response [responses req]
  (when-let [[k res] (->> (partition 2 responses)
                          (filter (comp (partial matches-request? req) first))
                          first)]
    [k (cond
         (map? res) res
         (ifn? res) (res req)
         :default res)]))

(defn log-request [spec req res])

(defn handle-request [responses req]
  (let [[spec res] (find-suitable-response responses req)]
    (log-request spec req res)
    (when-not spec
      (throw (ex-info (format "Unexpected request: %s %s"
                              (str/upper-case (name (:method req)))
                              (:url req))
                      {:req req})))
    res))

(defn stringify-request [spec & [req]]
  (let [spec (normalize-spec spec)
        method (:method spec (:method req))
        url (:url spec (:url req))
        matchers (dissoc spec :method :url :headers)]
    (str (when-not spec
           "[UNEXPECTED] ")
         (->> [(str (str/upper-case (name method)) " " url)
               (str/join "\n" (map #(str/join ": " %) (or (:headers req)
                                                      (:headers spec))))
               (str/join "\n" (map #(str/join " " %) matchers))]
              (remove empty?)
              (str/join "\n"))
         "\n")))

(defn indent [n s]
  (->> (str/split s #"\n")
       (map #(str (str/join (repeat n " ")) %))
       (str/join "\n")))

(defn verify-request-log [request-log response-stubs & [msg]]
  (let [provided-stubs (set (map first (partition 2 response-stubs)))
        requested-stubs (set (map :spec request-log))
        stubs-not-requested (set/difference provided-stubs requested-stubs)]
    (if (empty? stubs-not-requested)
      {:type :pass
       :message (str (when msg (str msg ": "))
                     "All response stubs were requested")
       :expected 'empty?
       :actual stubs-not-requested}
      {:type :fail
       :message (format (str (when msg (str msg ": "))
                             "%d response stub%s not requested:\n"
                             "%s\n\nReceived requests:\n%s\n")
                        (count stubs-not-requested)
                        (if (= 1 (count stubs-not-requested))
                          " was"
                          "s were")
                        (->> stubs-not-requested
                             (map #(stringify-request %))
                             (str/join "\n")
                             (indent 2))
                        (->> request-log
                             (map #(stringify-request (:spec %) (:req %)))
                             (str/join "\n")
                             (indent 2)))
       :expected #{}
       :actual stubs-not-requested})))

(defn verify-all-responses-requested [request-log response-stubs & [msg]]
  (let [res (verify-request-log @request-log @response-stubs msg)]
    (test/do-report res)
    (= :pass (:type res))))

(defn assert-all-responses-requested [& [msg]]
  (throw (Exception. "Wrap test code in with-request-log to use this assertion")))

(defn add-stubs [stubs])

(defmacro with-request-log [& forms]
  `(let [request-log# (atom [])
         response-stubs# (atom [])]
     (with-redefs [parrot.core/log-request
                   #(swap! request-log# conj {:spec %1 :req %2 :res %3})

                   parrot.core/add-stubs
                   #(swap! response-stubs# into %)

                   parrot.core/assert-all-responses-requested
                   (partial verify-all-responses-requested request-log# response-stubs#)]
       ~@forms)))
