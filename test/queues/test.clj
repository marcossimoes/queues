(ns queues.test
  (:require [clojure.test :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [queues.models.agent :as agent]
            [queues.models.job :as job]
            [queues.models.events :as events]
            [queues.models.specs :as specs]
            [queues.core]
            [queues.json]))

(def agent-ids (gen/sample (gen/not-empty (s/gen ::specs/agent.id)) 5))
(def job-ids (gen/sample (gen/not-empty (s/gen ::specs/job.id)) 5))
(def job-types (gen/sample (gen/not-empty (s/gen ::specs/job.type)) 3))

(def overrides
  {::specs/agent.id #(gen/elements agent-ids)
   ::specs/job.id   #(gen/elements job-ids)
   ::specs/job.type #(gen/elements job-types)})

(defn gen-events
  "generates a random vector of events with common
  - agent-ids
  - job-ids
  - job-types"
  [] (gen/vector (s/gen ::specs/event overrides) 0 100))

(stest/instrument)