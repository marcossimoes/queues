(ns queues.service-test
  (:require [clojure.spec.test.alpha :as stest]
            [midje.sweet :refer :all]
            [queues.service :refer :all]))

;;(facts "agents end-point"
;;       (fact "receives a put request of an agent returns successful"
;;             (handled-agent (mock/request :put "/agents")) => (contains {:status 200})))

;; TODO [TEST] service-test

(stest/instrument)