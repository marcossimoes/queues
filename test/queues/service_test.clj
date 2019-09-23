(ns queues.service-test
  (:require [clojure.spec.test.alpha :as stest]
            [io.pedestal.test :refer [response-for]]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.service :refer :all]))

(facts "get '/' endpoint"
       (fact "endpoint works"
             (response-for fix/tempserv :get "/") => (contains {:status 200})))

;; TODO [TEST] service-test

(stest/instrument)