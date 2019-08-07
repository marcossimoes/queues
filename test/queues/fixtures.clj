(ns queues.fixtures
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]
            [queues.init :as init]
            [queues.specs.agent :as specs.agent]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-queues :as specs.job-queues]))

(defn reset-job-queues!
  []
  (dosync
    (send-off init/agents #(or [] %))
    ;; FIXME: this solution to reset agents agent value seems weired
    (ref-set init/jobs-waiting [])
    (ref-set init/job-requests-waiting [])
    (ref-set init/jobs-assigned []))
  nil)

(defn sample-job-queues
  []
  {::specs.job-queues/agents (agent [])
   ::specs.job-queues/jobs-assigned (ref [])
   ::specs.job-queues/jobs-waiting (ref [])
   ::specs.job-queues/job-requests-waiting (ref [])
   ::specs.job-queues/jobs-done (agent [])
   ::specs.job-queues/priority-rules init/priority-rules})

(def agent-ids (gen/sample (gen/not-empty (s/gen ::specs.agent/id)) 5))
(def job-ids (gen/sample (gen/not-empty (s/gen ::specs.job/id)) 5))
(def job-types (gen/sample (gen/not-empty (s/gen ::specs.job/type)) 3))

(def overrides
  {::specs.agent/id #(gen/elements agent-ids)
   ::specs.job/id   #(gen/elements job-ids)
   ::specs.job/type #(gen/elements job-types)})

(defn gen-matching-events
  "generates a random vector of events with common
  - agent-ids
  - job-ids
  - job-types"
  [] (gen/vector (s/gen ::specs.events/event overrides) 0 100))

(stest/instrument)