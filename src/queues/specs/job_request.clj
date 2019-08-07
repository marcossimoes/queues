(ns queues.specs.job-request
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]))

(s/def ::agent-id ::specs.agent/id)
(s/def ::job-request (s/keys :req [::agent-id]))