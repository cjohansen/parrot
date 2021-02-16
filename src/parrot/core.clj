(ns parrot.core
  (:require [clojure.test :as test]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn matches-request? [req [method url attrs]]
  (let [criteria (cond-> attrs
                   method (assoc :method method)
                   url (assoc :url url))]
    (loop [ks (keys criteria)]
      (if (nil? ks)
        true
        (let [k (first ks)]
          (if (cond
                (= :headers k) (every? #(= (get-in criteria [k %])
                                           (get-in req [k %]))
                                       (keys (criteria k)))
                :default (= (criteria k) (req k)))
            (recur (next ks))
            false))))))

(defn find-suitable-response [responses req]
  (->> (partition 2 responses)
       (filter (comp (partial matches-request? req) first))
       first))

(defn prepare-response [stub]
  (merge
   {:status 404
    :headers {}
    :body {}}
   stub))

(defn log-request [spec req res])

(defn handle-request [responses req]
  (let [[spec stub] (find-suitable-response responses req)
        res (prepare-response stub)]
    (log-request spec req res)
    (when-not spec
      (throw (ex-info (format "Unexpected request: %s %s"
                              (str/upper-case (name (:method req)))
                              (:url req))
                      {:req req})))
    res))

(defn stringify-request [spec & [req]]
  (let [[method url specs] (or spec [(:method req :get) (:url req)])
        matchers (dissoc specs :headers)]
    (str (when-not spec
           "[UNEXPECTED] ")
         (->> [(str (str/upper-case (name method)) " " url)
               (str/join "\n" (map #(str/join ": " %) (or (:headers req)
                                                      (:headers specs))))
               (str/join "\n" (map #(str/join " " %) matchers))]
              (remove empty?)
              (str/join "\n"))
         "\n")))

(defn indent [n s]
  (->> (str/split s #"\n")
       (map #(str (str/join (repeat n " ")) %))
       (str/join "\n")))

(defn pass [params]
  (test/do-report (assoc params :type :pass))
  true)

(defn verify-all-responses-requested [request-log response-stubs & [msg]]
  (let [provided-stubs (set (map first @response-stubs))
        requested-stubs (set (map :spec @request-log))
        stubs-not-requested (set/difference provided-stubs requested-stubs)]
    (if (empty? stubs-not-requested)
      (pass {:message (str (when msg (str msg ": "))
                           "All response stubs were requested")
             :expected 'empty?
             :actual stubs-not-requested})
      (test/do-report
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
                         (->> @request-log
                              (filter (comp nil? :spec))
                              (map #(stringify-request (:spec %) (:req %)))
                              (str/join "\n")
                              (indent 2)))
        :expected #{}
        :actual stubs-not-requested}))))

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
