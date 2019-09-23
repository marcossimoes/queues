(ns queues.service-test
  (:require [clojure.spec.test.alpha :as stest]
            [io.pedestal.test :refer [response-for]]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.service :refer :all]))

(facts "get '/' endpoint"
       (fact "endpoint works"
             (response-for fix/tempserv :get "/") => (contains {:status 200}))
       (fact "if db is empty, return a map with keys jobs-done, jobs-being-done and jobs-queued"
             (response-for fix/tempserv :get "/") => (contains {:body "{\n  \"jobs_done\": [],\n  \"jobs_being_done\": [],\n  \"jobs_queued\": []\n}"})))



;; TODO [TEST] service-test

(stest/instrument)