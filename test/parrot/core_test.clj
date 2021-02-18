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

(deftest verify-all-responses-requested--ok
  (is (= (sut/verify-request-log
          [{:spec [:get "https://example.com/"]}]
          [[:get "https://example.com/"]
           {:status 200
            :body {:ok? true}}])
         {:type :pass
          :message "All response stubs were requested"
          :expected 'empty?
          :actual #{}})))

(deftest verify-all-responses-requested--missing-request-vector
  (is (= (sut/verify-request-log
          [{:spec [:get "https://example.com/"]
            :req {:method :get
                  :url "https://example.com/"}}]
          [[:get "https://example.com/"]
           {:status 200
            :body {:ok? true}}

           [:post "https://example.com/"]
           {:status 400
            :body {:ok? false}}])
         {:type :fail
          :message "1 response stub was not requested:\n  POST https://example.com/\n\nReceived requests:\n  GET https://example.com/\n"
          :expected #{}
          :actual #{[:post "https://example.com/"]}})))

(deftest verify-all-responses-requested--missing-request-map
  (is (= (sut/verify-request-log
          [{:spec [:get "https://example.com/"]
            :req {:method :get
                  :url "https://example.com/"}}]
          [[:get "https://example.com/"]
           {:status 200
            :body {:ok? true}}

           {:method :post
            :url "https://example.com/"}
           {:status 400
            :body {:ok? false}}])
         {:type :fail
          :message "1 response stub was not requested:\n  POST https://example.com/\n\nReceived requests:\n  GET https://example.com/\n"
          :expected #{}
          :actual #{{:method :post, :url "https://example.com/"}}})))

(deftest verify-all-responses-requested--unexpected-request
  (is (= (sut/verify-request-log
          [{:spec [:get "https://example.com/"]
            :req {:method :get
                  :url "https://example.com/"}}
           {:req {:method :post
                  :url "https://lol.com"}}]
          [[:get "https://example.com/"]
           {:status 200
            :body {:ok? true}}

           {:method :post
            :url "https://example.com/"}
           {:status 400
            :body {:ok? false}}])
         {:type :fail
          :message "1 response stub was not requested:\n  POST https://example.com/\n\nReceived requests:\n  GET https://example.com/\n  \n  [UNEXPECTED] POST https://lol.com\n"
          :expected #{}
          :actual #{{:method :post
                     :url "https://example.com/"}}})))
