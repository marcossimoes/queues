(ns queues.logic.jobs-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.logic.jobs :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
    ;; Although undetected by IntelliJ job-assigned spec require is used
            [queues.specs.job-assigned :as specs.job-assigned]))
(let [job (gen/generate (s/gen ::specs.job/job))
      job-id (::specs.job/id job)
      matching-job-type (::specs.job/type job)
      matching-urgent (::specs.job/urgent job)
      non-matching-urgent (not matching-urgent)
      agent-id (gen/generate (s/gen ::specs.agents/id))
      matching-skill matching-job-type
      non-matching-skill (str matching-skill "str-to-make-skill-diff")
      job-with-assigned-agent-id (assoc job ::specs.job/assigned-agent agent-id)
      job-assigned #::specs.job-assigned{:job-assigned #::specs.job-assigned{:job-id   job-id
                                                                             :agent-id agent-id}}]
  (facts "job-with-agent-assigned"
         (fact "returns job with agent-id in assigned agent"
               (job-with-agent-assigned agent-id job) => job-with-assigned-agent-id))
  (facts "job-assigned-map-from-job-with-assigned-agent"
         (fact "returns job-assigned event"
               (job-assigned-map-from-job-with-assigned-agent job-with-assigned-agent-id) => job-assigned))
  (facts "job-assigned-map-from-agent-and-job-ids"
         (fact "returns job-assigned event"
               (#'queues.logic.jobs/job-assigned-map-from-agent-and-job-ids agent-id job-id) => job-assigned))
  (facts "job-matches-skill-and-urgency?"
         (fact "only returns true if both skill and urgent matches"
               (job-matches-skill-and-urgency? job matching-skill matching-urgent) => true
               (job-matches-skill-and-urgency? job non-matching-skill matching-urgent) => false
               (job-matches-skill-and-urgency? job matching-skill non-matching-urgent) => false
               (job-matches-skill-and-urgency? job non-matching-skill non-matching-urgent) => false)))

;; TODO [QUESTION; TEST] for simpler functions handling specs is it okay to only test through fdef and stest/instrument?

(stest/instrument)