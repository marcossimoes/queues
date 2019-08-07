(ns queues.specs.job-assigned
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]))

(s/def ::job-id ::specs.job/id)
(s/def ::agent-id ::specs.agent/id)
(s/def ::job-assigned (s/keys :req [::job-id
                                    ::agent-id]))