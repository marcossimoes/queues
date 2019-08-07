(ns queues.app-test
  (:require [clojure.spec.test.alpha :as stest]
            [midje.sweet :refer :all]
            [queues.app :refer :all]))

;;(facts "agents end-point"
;;       (fact "receives a put request of an agent returns successful"
;;             (handled-agent (mock/request :put "/agents")) => (contains {:status 200})))

(stest/instrument)