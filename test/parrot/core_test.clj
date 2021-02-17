(ns parrot.core-test
  (:require [parrot.core :as sut]
            [clojure.test :refer [deftest is]]))

(deftest matches-vector-form
  (is (= (sut/find-suitable-response
          [[:get "https://example.com/"]
           {:status 200
            :body {:ok? true}}]
          {:method :get
           :url "https://example.com/"})
         [[:get "https://example.com/"]
          {:status 200
           :body {:ok? true}}])))

(deftest matches-header-subset
  (is (= (sut/find-suitable-response
          [{:headers {"content-type" "application/json"}}
           {:status 201}]
          {:method :get
           :url "https://example.com"
           :headers {"Authorization" "Bearer ..."
                     "Content-Type" "application/json"}})
         [{:headers {"content-type" "application/json"}}
          {:status 201}])))

(deftest matches-header-subset-with-regex
  (is (= (second
          (sut/find-suitable-response
           [{:headers {"content-type" #"json"}}
            {:status 201}]
           {:method :get
            :url "https://example.com"
            :headers {"Authorization" "Bearer ..."
                      "Content-Type" "application/json"}}))
         {:status 201})))
