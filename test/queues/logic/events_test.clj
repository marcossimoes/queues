(ns queues.logic.events-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [queues.fixtures :as fix]
            [queues.logic.events :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-req]))

(let [new-agent-event (gen/generate (s/gen ::specs.events/new-agent-event))
      new-job-event (gen/generate (s/gen ::specs.events/new-job-event))
      job-req-event (gen/generate (s/gen ::specs.events/job-request-event))]
  (facts "event-type"
         (fact "if receives a new-agent returns '::specs.events/new-agent'"
               (event-type new-agent-event) => ::specs.events/new-agent)
         (fact "if receives a new-job returns '::specs.events/new-job'"
               (event-type new-job-event) => ::specs.events/new-job)
         (fact "if receives a job-req returns '::specs.events/job-request'"
               (event-type job-req-event) => ::specs.events/job-request)))

(stest/instrument)