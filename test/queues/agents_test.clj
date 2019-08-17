(ns queues.agents-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [queues.init :as init]
            [queues.logic.agents :refer :all]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-queues :as specs.job-queues]))

(let [job (gen/generate (s/gen ::specs.job/job))
      agent (-> (gen/generate (s/gen ::specs.agent/agent))
                (assoc ::specs.agent/primary-skillset [(::specs.job/type job)]))
      job-req-payload {::specs.job-request/agent-id (::specs.agent/id agent)}
      jqs-with-jobs-waiting (-> init/job-queues
                                (update ::specs.job-queues/agents #(send % conj agent))
                                ((fn [jqs]
                                   (dosync
                                     (update jqs ::specs.job-queues/jobs-waiting
                                             #(alter % conj job))))))]
  (facts "agent-found"
         (fact "if agents and jobs has the provided agent id returns agent"
               (agent-found jqs-with-jobs-waiting job-req-payload) => agent)))

(facts "agent-skillsets"
       (fact "if agent does not have a secondary skill does not return nil for the skill missing"
             (agent-skillsets {::specs.agent/primary-skillset ["rewards-questions"]
                               ::specs.agent/secondary-skillset []})
             => ["rewards-questions"])
       (fact "returns a vector with both skills in one single coll"
             (agent-skillsets {::specs.agent/primary-skillset ["rewards-questions"]
                               ::specs.agent/secondary-skillset ["bills-questions"]})
             => ["rewards-questions" "bills-questions"]))

(stest/instrument)