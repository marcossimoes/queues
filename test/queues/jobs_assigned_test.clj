(ns queues.jobs-assigned-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.init :as init]
            [queues.logic.jobs-assigned :refer :all]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-queues :as specs.job-queues]))

(let [job (gen/generate (s/gen ::specs.job/job))
      agent (-> (gen/generate (s/gen ::specs.agent/agent))
                (assoc ::specs.agent/primary-skillset [(::specs.job/type job)]))
      job-req-payload {::specs.job-request/agent-id (::specs.agent/id agent)}
      job-assigned {::specs.job-assigned/job-assigned {::specs.job-assigned/job-id   (::specs.job/id job)
                                                       ::specs.job-assigned/agent-id (::specs.agent/id agent)}}]
  (facts "added-job-assigned"
         (fact
           (dosync
             (added-job-assigned init/job-queues job-req-payload job)
             (last @init/jobs-assigned) => job-assigned))))
(facts "id-removed-from-job-queue"
       (fact "if it receives an id and a list of vectors with maps one of them containing that id, removes it"
             (let [jobs-waiting-vec [{::specs.job/id "1"} {::specs.job/id "2"} {::specs.job/id "3"} {::specs.job/id "4"}]]
               (dosync
                 (alter init/jobs-waiting (fn [_] jobs-waiting-vec))
                 (id-removed-from-job-queue init/jobs-waiting "1" ::specs.job/id))
               (-> init/jobs-waiting
                   deref
                   count
                   inc) => (count jobs-waiting-vec))))

(facts "assigned-job"
       (fact "If it receives an 'job-queues' map, a job request payload and a job
         returns a new 'job-queues' maps with a new job-assigned event containing
         the previously inputed 'job-request-agent-id' and the 'job-id'"
             (dosync
               (assigned-job init/job-queues
                             {::specs.job-request/agent-id "2"}
                             {::specs.job/id "1" ::specs.job/type "bills" ::specs.job/urgent true})
               (-> init/job-queues ::specs.job-queues/jobs-assigned deref))
             => (contains {::specs.job-assigned/job-assigned {::specs.job-assigned/job-id   "1"
                                                              ::specs.job-assigned/agent-id "2"}})))

(stest/instrument)