(ns queues.logic.agents
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job-queues :as specs.job-queues]
            [queues.specs.job-request :as specs.job-request]))

(defn agent-found
  "Receives job-queues and a job-request content and returns the agent related
  to that job request or nil if no agent is found"
  [job-queues job-req-content]
  (let [agent-id ((comp first vals) job-req-content)
        agents (::specs.job-queues/agents job-queues)]
    (some (fn [agent]
            (when (= agent-id (::specs.agent/id agent))
              agent))
          @agents)))

;;(s/fdef agent-found
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-req-content ::specs.job-request/job-req)
;;        :ret (s/or :found ::specs.agent/agent
;;                   :not-found nil?)
;;        :fn (s/or :found #(= (-> % :ret :found ::specs.agent/id)
;;                             (-> % :args :job-req-content ::specs.job-request/agent-id))
;;                  :not-found #(nil? (-> :ret :not-found %))))

(defn agent-skillsets
  "Receives an agent an returns a coll with its skillsets"
  [agent]
  (concat (::specs.agent/primary-skillset agent)
          (::specs.agent/secondary-skillset agent)))

;; FIXME: when new-agent is entered check for corresponding job-requestand new-jobs
;; FIXME: when an agent id or job id is entered for the second time, update the original agent/id
;; TODO: implement agents as a map of agents ids and agents as values
;; TODO: implement the alternative of multiple skills in the primary or secondary skillset vectors