(ns queues.init
  (:require [queues.specs.job-queues :as specs.job-queues]
            [queues.specs.priority-rule :as specs.priority-rule]
            [queues.specs.agent :as specs.agent]))

(def ^:dynamic *logging* false)
(def agents (agent {}))
(def jobs-assigned (ref []))
(def jobs-waiting (ref []))
(def job-requests-waiting (ref []))
(def jobs-done (agent []))
(def priority-rules [{::specs.priority-rule/skill-type ::specs.agent/primary-skillset ::specs.priority-rule/urgent true}
                     {::specs.priority-rule/skill-type ::specs.agent/primary-skillset ::specs.priority-rule/urgent false}
                     {::specs.priority-rule/skill-type ::specs.agent/secondary-skillset ::specs.priority-rule/urgent true}
                     {::specs.priority-rule/skill-type ::specs.agent/secondary-skillset ::specs.priority-rule/urgent false}])

(def job-queues {::specs.job-queues/agents agents
                 ::specs.job-queues/jobs-assigned jobs-assigned
                 ::specs.job-queues/jobs-waiting jobs-waiting
                 ::specs.job-queues/job-requests-waiting job-requests-waiting
                 ::specs.job-queues/jobs-done jobs-done
                 ::specs.job-queues/priority-rules priority-rules})

(def default-opts {:input-file   "resources/sample-input.json.txt"
                   :log          false
                   :pretty-print false
                   :output-file  "jobs-assigned.json.txt"
                   :web-server false})

