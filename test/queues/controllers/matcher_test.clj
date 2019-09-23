(ns queues.controllers.matcher-test
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [midje.sweet :refer :all]
            [queues.controllers.matcher :refer :all]
            [queues.init :as init]
            [queues.specs.agents :as specs.agents]
            [queues.specs.agents :as specs.agents]
            [queues.specs.queues :as specs.queues]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.test-cases :as cases]))

(let [agent-with-job-req-queued #'queues.controllers.matcher/agent-with-job-req-queued
      agents-from-job-reqs #'queues.controllers.matcher/agents-from-job-reqs]
  (facts "agent-with-job-req-queued"
         (fact "if agents is empty returns nil"
               (agent-with-job-req-queued (init/db) cases/job-req-p1) => nil)
         (fact "if no agent matches job-req returns nil"
               (let [db-data {::specs.agents/all-agents {(::specs.agents/id cases/agent-p12)   cases/agent-p12
                                                         (::specs.agents/id cases/agent-p1-s6) cases/agent-p1-s6}}]
                 (agent-with-job-req-queued (init/db db-data) cases/job-req-p1) => nil))
         (fact "If the job-req has a matching agent returns it"
               (let [db-data {::specs.agents/all-agents {(::specs.agents/id cases/agent-p1)    cases/agent-p1
                                                         (::specs.agents/id cases/agent-p12)   cases/agent-p12
                                                         (::specs.agents/id cases/agent-p1-s6) cases/agent-p1-s6}}]
                 (agent-with-job-req-queued (init/db db-data) cases/job-req-p1) => cases/agent-p1)))
  (facts "agents-from-job-reqs"
         (fact "if job-reqs-queued is empty, returns and empty coll"
               (agents-from-job-reqs (init/db) cases/job-reqs-queued-empty) => #(empty? %))
         (fact "if all job-reqs in job-reqs-queued match an agent in agents return all the agents"
               (let [db-data {::specs.agents/all-agents {(::specs.agents/id cases/agent-p1)    cases/agent-p1
                                                         (::specs.agents/id cases/agent-p12)   cases/agent-p12
                                                         (::specs.agents/id cases/agent-p1-s6) cases/agent-p1-s6}}]
                 (agents-from-job-reqs (init/db db-data) cases/job-reqs-p1-p12-p1s6) => cases/agents-p1-p12-p1s6))
         (fact "if not all job-reqs in job-reqs-queued match agents return just the matched agents"
               (let [db-data {::specs.agents/all-agents {(::specs.agents/id cases/agent-p1)  cases/agent-p1
                                                         (::specs.agents/id cases/agent-p12) cases/agent-p12}}]
                 (agents-from-job-reqs (init/db db-data) cases/job-reqs-p1-p12-p1s6) => cases/agents-p1-p12)))
  (facts "queued-object-matching"
         (fact "if db is EMPTY returns nil"
               (queued-object-matching (init/db) cases/new-job-clj-event-1t) => nil
               (queued-object-matching (init/db) cases/job-req-clj-event-p1) => nil)
         (fact "if db HAS NO matching waiting event, returns nil"
               (let [db-data {::specs.agents/all-agents      {(::specs.agents/id cases/agent-p1) cases/agent-p1}
                              ::specs.queues/job-reqs-queued [cases/job-req-p1]}]
                 (queued-object-matching (init/db db-data) cases/new-job-clj-event-2t) => nil)
               (let [db-data {::specs.agents/all-agents   {(::specs.agents/id cases/agent-p1) cases/agent-p1}
                              ::specs.queues/jobs-waiting [cases/job-2t]}]
                 (queued-object-matching (init/db db-data) cases/job-req-clj-event-p1) => nil))
         (fact "if db HAS matching waiting event, returns it"
               (let [db-data {::specs.agents/all-agents      {(::specs.agents/id cases/agent-p1) cases/agent-p1}
                              ::specs.queues/job-reqs-queued [cases/job-req-p1]}]
                 (queued-object-matching (init/db db-data) cases/new-job-clj-event-1t) => cases/job-req-p1)
               (let [db-data {::specs.agents/all-agents   {(::specs.agents/id cases/agent-p1) cases/agent-p1}
                              ::specs.queues/jobs-waiting [cases/job-1t]}]
                 (queued-object-matching (init/db db-data) cases/job-req-clj-event-p1) => cases/job-1t))
         (fact "if db HAS MULTIPLE matching waiting event, returns the first one"
               (let [db-data {::specs.agents/all-agents      {(::specs.agents/id cases/agent-p1)   cases/agent-p1,
                                                              (::specs.agents/id cases/agent-p5=1) cases/job-req-p5=1}
                              ::specs.queues/job-reqs-queued [cases/job-req-p1 cases/agent-p5=1-id]}]
                 (queued-object-matching (init/db db-data) cases/new-job-clj-event-1t) => cases/job-req-p1)
               (let [db-data {::specs.agents/all-agents   {(::specs.agents/id cases/agent-p1) cases/agent-p1}
                              ::specs.queues/jobs-waiting [cases/job-1t cases/job-5t=1t]}]
                 (queued-object-matching (init/db db-data) cases/job-req-clj-event-p1) => cases/job-1t))))

;; TODO [TEST] property tests for controllers.matcher-test

(stest/instrument)