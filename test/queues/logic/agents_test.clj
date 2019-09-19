(ns queues.logic.agents-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [queues.logic.agents :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job-request :as specs.job-request]))

(let [agent (gen/generate (s/gen ::specs.agents/agent))
      id (::specs.agents/id agent)]
  (facts "job-req-from-agent"
         (fact "if receives an agent with agent-id, produces a
         job request with the corresponding id"
           (-> agent
               job-req-from-agent
               ::specs.job-request/agent-id) => id)))

(stest/instrument)