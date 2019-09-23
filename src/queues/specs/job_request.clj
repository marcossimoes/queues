(ns queues.specs.job-request
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]))

(s/def ::agent-id ::specs.agents/id)
(s/def ::job-request (s/keys :req [::agent-id]))