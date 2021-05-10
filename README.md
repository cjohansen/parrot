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
consider the return value.

`with-request-log` can span multiple `with-responses`. In other words you can
write a longer integration style test, wrapping individual steps in a dedicated
`with-responses`, and wrap the entire thing in a `with-request-log` and include
`assert-all-responses-requested` at the end of the test.

## Install

Parrot HTTP is a stable library - it will never change it's public API in a
breaking way, and will never (intentionally) introduce other breaking changes.

With tools.deps:

```clj
cjohansen/parrot-http {:mvn/version "2021.02.18"}
```

With Leiningen:

```clj
[cjohansen/parrot-http "2021.02.18"]
```

**NB!** Please do not be alarmed if the version/date seems "old" - this just
means that no bugs have been discovered in a while. Parrot HTTP is largely
feature-complete, and I expect to only rarely add to its feature set.

<a id="specs"></a>
## Request matching

`with-responses` takes a vector of "request spec"/response pairs. The request
spec can either be vector specifying the request method and URL, or a map that
matches against any property of the request:

```clj
(with-responses
  [[:get "https://example.com/"]
   {:status 200
    :body {:ok? true}}

   {:method :post}
   {:status 400}

   {:method :get
    :url "http://test.com"
    :headers {"content-type" "application/json"}}
   {:status 200
    :body {:json? true}}]

  ,,,)
```

Parrot will always select the first request that matches, so in case of
overlapping matches you should go in order of most to least specific.

The spec `[:get "https://example.com/"]` is equivalent to `{:method :get, :url
"https://example.com"}`.

Parrot defaults to checking that the value in the spec is the same in the
request, e.g. `(= (:method spec) (:method req))`. If all the spec criteria
matches, the paired response is used.

Some spec keys are treated differently, as determined by the multi-method
`(parrot.core/match? k spec req)`:

- `:headers` If each header in the spec is the same as the header in the
  request, it is a match - even if the spec does not specify all headers in the
  request. Header names are compared case-insensitively.

```clj
(require '[parrot.clj-http :refer [with-responses]]
         '[clj-http.client :as http])

(with-responses
 [{:headers {"content-type" "application/json"}}
  {:status 201}]

 (http/request
  {:method :get
   :url "https://example.com"
   :headers {"Authorization" "Bearer ..."
             "Content-Type" "application/json"}})
 ;;=> {:status 201}
)
```

The spec may also specify regular expressions in place of string values to
perform a fuzzy match:

```clj
(with-responses
 [{:headers {"content-type" #"json"}}
            {:status 201}]

 ,,,)
```

## Changelog

### 2021.02.18

Initial version

## License

Copyright © 2021 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
