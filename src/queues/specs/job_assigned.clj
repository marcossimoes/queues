(ns queues.specs.job-assigned
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]))

(s/def ::job-id ::specs.job/id)
(s/def ::agent-id ::specs.agents/id)
(s/def ::job-assigned (s/keys :req [::job-id
                                    ::agent-id]))
(s/def ::job-assigned-map (s/keys :req [::job-assigned]))