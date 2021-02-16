# Parrot - HTTP response stubs for Clojure

Do you shy away from writing tests for code that makes HTTP requests? Well, no
more. Parrot HTTP gives you a convenient way to provide canned responses for
matching requests in your tests, and provides assertions to verify that your
code is making the right requests in the right way.

## Talk to me, Parrot

Parrot's `with-responses` macro allows you to define pairs of [request
specs](#specs) and responses. Matching requests will be served the canned
response.

```clj
(require '[parrot.clj-http :refer [with-responses]]
         '[clj-http.client :as http])

(with-responses
  [[:get "http://localhost:9012"]
   {:status 200
    :body {:ok? true}}]

  (http/request {:method :get
                 :url "http://localhost:9012"})
  ;;=>
  ;;{:status 200
  ;; :body {:ok? true}}
  )
```

Code in `with-responses` is not expected to make actual HTTP requests, so if a
request is made that doesn't match any of the inline respones, an exception is
thrown.

You might want to make sure that all the specified responses have been served.
To do this you need to build a request log with `with-request-log` and include
an assertion:

```clj
(require '[parrot.clj-http :refer [with-responses
                                   with-request-log
                                   assert-all-responses-requested]]
         '[clj-http.client :as http]
         '[clojure.test :refer [deftest is])

(deftest makes-some-requests
  (with-request-log
    (is (= (with-responses
             [[:get "http://localhost:9012"]
              {:status 200
               :body {:ok? true}}]

             (http/request {:method :get
                            :url "http://localhost:9012"}))
           {:status 200
            :body {:ok? true}}))

    (assert-all-responses-requested)))
```

Placing `with-responses` as close to the code making the requests as possible
will give you a better REPL experience, as you can evaluate the expression to
consider the return value. `with-request-log` can span multiple
`with-responses`. In other words you can write a longer integration style test,
wrapping individual steps in a dedicated `with-responses`, and wrap the entire
thing in a `with-request-log` and include `assert-all-responses-requested` at
the end of the test.

## Install

Parrot HTTP is a stable library - it will never change it's public API in a
breaking way, and will never (intentionally) introduce other breaking changes.

With tools.deps:

```clj
cjohansen/parrot-http {:mvn/version "2021.02.16"}
```

With Leiningen:

```clj
[cjohansen/parrot-http "2021.02.16"]
```

**NB!** Please do not be alarmed if the version/date seems "old" - this just
means that no bugs have been discovered in a while. Parrot HTTP is largely
feature-complete, and I expect to only rarely add to its feature set.

## Changelog

### 2021.02.16

Initial version

## License

Copyright © 2021 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
