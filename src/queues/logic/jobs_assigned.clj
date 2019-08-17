(ns queues.logic.jobs-assigned
  (:require [clojure.spec.alpha :as s]
            [queues.logic.agents :as agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-queues :as specs.job-queues]))

(defn added-job-assigned
  "Receives a job to be assigned to an agent and returns a function that
  creates a job assigned object and conjures it to an afterwards provided
  jobs-assigned vector"
  [job-queues job-request-payload job]
  (let [job-id (::specs.job/id job)
        agent-id (::specs.job-request/agent-id job-request-payload)
        job-assigned {::specs.job-assigned/job-assigned {::specs.job-assigned/job-id job-id
                                                         ::specs.job-assigned/agent-id agent-id}}]
    (alter (::specs.job-queues/jobs-assigned job-queues) conj job-assigned)))

;;(s/fdef update-job-assigneds-func
;;        :args (s/cat :job ::specs.job/job
;;                     :job-req-payload ::specs.job-request/job-req)
;;        :ret (s/fspec :args (s/cat :jobs-assigned ::specs.job-queues/jobs-assigned)
;;                      :ret ::specs.job-queues/jobs-assigned
;;                      :fn #(= (-> % :ret drop-last)
;;                              (-> % :args :jobs-assigned))))

(defn id-removed-from-job-queue
  "Receives a job-id and returns a function that takes a vector containing jobs with ids
  and returns a new vector with all the elements of the original vector but the map
  with the id provided to build the function"
  [job-queue id id-type]
  (alter job-queue
         (fn [org-jq]
           (reduce (fn [new-jq m]
                     (if (= (id-type m) id)
                       new-jq
                       (conj new-jq m)))
                   []
                   org-jq))))

;;(s/fdef id-removed-from-vector
;;        :args (s/cat :id string?
;;                     :id-type keyword?)
;;        :ret (s/fspec :args (s/cat :org-vector coll?)
;;                      :ret coll?
;;                      :fn #(= 1 (- (count (-> % :args :org-vector))
;;                                   (count (-> % :ret))))))

(defn assigned-job
  "Receives job-queues and a job request payload and returns
  job-queues with a job assigned with that job request id and
  that job removed from job-waiting"
  [job-queues job-req-payload job]
  (let [job-assigned (-> job-queues
                         (added-job-assigned job-req-payload job)
                         (last))]
    (agents/agent-in-job-queues-with-status job-queues job-req-payload ::busy job)
    (id-removed-from-job-queue (::specs.job-queues/jobs-waiting job-queues)
                               (::specs.job/id job)
                               ::specs.job/id)
    (id-removed-from-job-queue (::specs.job-queues/job-requests-waiting job-queues)
                               (::specs.job-request/agent-id job-req-payload)
                               ::specs.job-request/agent-id)
    job-assigned))

;;(s/fdef assigned-job
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-req-payload ::specs.job-request/job-req
;;                     :job ::specs.job/job)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/and #(= 1 (- (-> % :args :job-queues ::specs.job-queues/job-requests-waiting)
;;                            (-> % :ret ::specs.job-queues/job-requests-waiting)))
;;                   #(= 1 (- (-> % :args :job-queues ::specs.job-queues/jobs-waiting)
;;                            (-> % :ret ::specs.job-queues/jobs-waiting)))
;;                   #(= 1 (- (-> % :ret ::specs.job-queues/jobs-assigned)
;;                            (-> % :args :job-queues ::specs.job-queues/jobs-assigned)))))