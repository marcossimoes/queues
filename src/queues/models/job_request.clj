(ns queues.models.job-request
  (:require [clojure.spec.alpha :as s]
            [queues.models.agent :as agent]))

(s/def ::agent-id ::agent/id)

(s/def ::job-request (s/keys :req [::agent-id]))