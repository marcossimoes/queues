(ns queues.core
  (:require [clojure.pprint :as pp]
            [queues.cli :as cli]
            [queues.init :as init]
            [queues.io :as io]
            [queues.logic.events :as events]
            [queues.server :as server]
            [queues.service :as service]
            [queues.specs.job-queues :as specs.job-queues])
  (:gen-class))

(defn added-bulk-events
  "Processes multiple events in bulk from a file"
  [clj-events job-queues options]
  (binding [init/*logging* (:log options)]
    ;;(println "clj-events: " (map (fn [event] (events/added-event-with-log job-queues event)) clj-events))
    (run! #(events/added-event-with-log job-queues %) clj-events)))

(defn processed-job-queues
  "Creates job-queues from events provided either from a input file or a web server API"
  [job-queues options]
  ;;(println "options: " options)
  (cond
    (:web-server options) (server/start)
    :else (-> options
              (:input-file)
              (io/read-json-file)
              (added-bulk-events job-queues options))))

(defn -main
  [& args]
  (let [options (cli/processed-args args init/default-opts)
        job-queues (do (processed-job-queues init/job-queues options)
                       (deref (::specs.job-queues/jobs-assigned init/job-queues)))]
    (when (:pretty-print options) (pp/pprint job-queues))
    (io/write-json-file job-queues (:output-file options))))

;;TODO: better separate concerns - move all agent/ref handling functions to a controller namespace
;;TODO: refactor file reading to use buffer and edn
;;TODO: include time stamp in the beginning of output file name so if you run the program multiple times it does not overrides the previous output file