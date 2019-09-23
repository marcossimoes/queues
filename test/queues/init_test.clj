(ns queues.init-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.init :refer :all]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.queues :as specs.queues]
            [queues.specs.order-of-priority :as specs.order-of-priority]))

(let [agents (gen/generate (s/gen ::specs.agents/all-agents))
      jobs-waiting (gen/generate (s/gen ::specs.queues/jobs-waiting))
      jobs-in-progress (gen/generate (s/gen ::specs.queues/jobs-in-progress))
      jobs-done (gen/generate (s/gen ::specs.queues/jobs-done))
      job-reqs-queued (gen/generate (s/gen ::specs.queues/job-reqs-queued))
      order-of-priority (gen/generate (s/gen ::specs.order-of-priority/order-of-priority))]
  (facts "db"
         (fact "creates db with empty values if not inital values are provided"
               (-> (db) ::specs.db/agents deref) => {})
         (fact "creates db with provided agents inside an object"
               (-> {::specs.agents/all-agents agents}
                   (db)
                   ::specs.db/agents
                   deref) => agents)
         (fact "creates db with provided jobs-waiting inside an object"
               (-> {::specs.queues/jobs-waiting jobs-waiting}
                   (db)
                   ::specs.db/jobs-waiting
                   deref) => jobs-waiting)
         (fact "creates db with provided jobs-in-progress inside an object"
               (-> {::specs.queues/jobs-in-progress jobs-in-progress}
                   (db)
                   ::specs.db/jobs-in-progress
                   deref) => jobs-in-progress)
         (fact "creates db with provided jobs-done inside an object"
               (-> {::specs.queues/jobs-done jobs-done}
                   (db)
                   ::specs.db/jobs-done
                   deref) => jobs-done)
         (fact "creates db with provided job-reqs-queued inside an object"
               (-> {::specs.queues/job-reqs-queued job-reqs-queued}
                   (db)
                   ::specs.db/job-reqs-queued
                   deref) => job-reqs-queued)
         (fact "creates db with provided order-of-priority inside an object"
               (-> {::specs.order-of-priority/order-of-priority order-of-priority}
                   (db)
                   ::specs.db/order-of-priority) => order-of-priority)))

;; TODO [QUESTION] should this tests be transformed into property tests since they are using random values?

(stest/instrument)