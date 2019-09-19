(ns queues.controllers.batch-processor
  (:require [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [queues.cli :as cli]
            [queues.init :as init]
            [queues.io :as io]
            [queues.json-converter :as json-converter]
            [queues.logic.jobs :as logic.jobs]
            [queues.specs.db :as specs.db]
            [queues.specs.event-processor :as specs.event-processor]
            [queues.specs.events :as specs.events]
            [queues.specs.jobs-assigned :as specs.jobs-assigned]
            [queues.state :as state]))

(defn populate-db-applying-event-processor-on-batch-events!
  [db event-processor batch-events]
  (run! #(event-processor db %) batch-events))

;; FIXME spec above failing when calling tests of batch-processor
;;(s/fdef populate-db-applying-event-processor-on-batch-events!
;;        :args (s/cat :db ::specs.db/db
;;                     :event-processor ::specs.event-processor/event-processor
;;                     :batch-events ::specs.events/events))

(defn- all-job-assigned-events
  [db]
  ;; FIXME this structure does not guarantee that the jobs assigned will be delivered in the
  ;; order they were assigned. If a job was assigned first but then finished later it will be
  ;; displayed first in this vector
  (let [all-jobs-with-assigned-agent (concat (state/all-jobs-done db)
                                             (state/all-jobs-in-progress db))]
    (map logic.jobs/job-assigned-map-from-job-with-assigned-agent
         all-jobs-with-assigned-agent)))

(s/fdef all-job-assigned-events
        :args (s/cat :db ::specs.db/db)
        :ret ::specs.jobs-assigned/jobs-assigned
        :fn #(= (+ (-> % :args :db ::specs.db/jobs-in-progress deref count)
                   (-> % :args :db ::specs.db/jobs-done deref count))
                (-> % :ret count)))

(defn- job-assigned-events-created-by-event-processor-from-new-events-batch!
  [db event-processor clj-events]
  (populate-db-applying-event-processor-on-batch-events! db event-processor clj-events)
  (all-job-assigned-events db))

;; FIXME spec above failing when calling tests of batch-processor

;;(s/fdef job-assigned-events-created-by-event-processor-from-new-events-batch!
;;        :args (s/cat :db ::specs.db/db
;;                     :event-processor ::specs.event-processor/event-processor
;;                     :clj-events ::specs.events/events)
;;        :ret ::specs.jobs-assigned/jobs-assigned
;;        :fn #(> (-> % :args :clj-events count)
;;                (-> % :ret count)))

(defn- outputs-job-assigned-json-formatted-events-to-output-options!
  ;; explicit output-options variable relevant for spec check
  [{:keys [pretty-print output-file], :as output-options}
   jobs-assigned]
  ;; TODO [QUESTION; ARCH] should this when bellow be in cli or in io?
  (when pretty-print (cli/print-job-queue-to-console! jobs-assigned))
  ;; mandatory options, always executed
  (-> jobs-assigned
      (json-converter/json-events-str-formatted-from-clj-events)
      (io/output-job-queue-str-formatted-to-file! output-file)))

(s/fdef outputs-job-assigned-json-formatted-events-to-output-options!
        :args (s/cat :output-options map?
                     :jobs-assigned ::specs.jobs-assigned/jobs-assigned))

(defn- new-clj-events-batch-from-file [input-file]
  ;; TODO [IMPROVE; ERROR HANDLING] if file is not the type supported ask user for a new file
  (when-let [json-events-str (io/str-from-json-file-content input-file)]
    (json-converter/clj-events-from-json-events-str json-events-str)))

(s/fdef new-clj-events-batch-from-file
        :args (s/cat :input-file string?)
        :ret ::specs.events/events)

(defn outputs-jobs-assigned-created-by-event-processor-from-file-to-output-options!
  [event-processor {:keys [input-file], :as options}]
  ;; it is fundamental for the batch processing that this initialization returns an empty db so as to guarantee that the batch processor
  ;; only outputs the events regarding the information outputed in the file
  ;; TODO [IMPROVE; ARCH] Should db be initialized in the same place regardless of being a webserver or a batchfile processing?
  (if-let [new-events-batch (new-clj-events-batch-from-file input-file)]
    (do (println "new-events-batch: " new-events-batch)
        (let [db (init/db)
              jobs-assigned (job-assigned-events-created-by-event-processor-from-new-events-batch! db
                                                                                                   event-processor
                                                                                                   new-events-batch)]
          (outputs-job-assigned-json-formatted-events-to-output-options! options
                                                                         jobs-assigned)))))

;;(s/fdef outputs-jobs-assigned-created-by-event-processor-from-file-to-output-options!
;;        :args (s/cat :event-processor (s/fspec :args (s/cat :db ::specs.db/db
;;                                                            :event ::specs.events/event))
;;                     :options map?))

;; TODO [QUESTION; ARCH] is this namespace really a controller or given its nature is there other concept I could use here?
;; TODO [QUESTION; READ] is it ok for all function names to be so long?
;; TODO [NXT] functions that returns how many jobs of each type an agent has performed