(ns parrot.clj-http
  (:require [parrot.core :as parrot]
            [realize.core :as realize]))

(defmacro with-responses [responses & forms]
  `(do
     (require 'clj-http.client)
     (with-redefs [clj-http.client/request (partial parrot/handle-request ~responses)]
       (parrot/add-stubs ~responses)
       (realize/realize (do ~@forms)))))

(defmacro with-request-log [& forms]
  `(parrot/with-request-log ~@forms))

(defn assert-all-responses-requested [& [msg]]
  (parrot/assert-all-responses-requested msg))
