(ns queues.cli
  (:require [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [queues.specs.events :as specs.events]
            [queues.specs.db :as specs.db]))

(defn processed-args
  "Receives an args vector with different strings corresponding to different run options
  and parses this options returning a map containing the configured options accordingly to the
  args vector"
  [rem-args processed-input]
  (cond
    (contains? #{"-l" "--log"} (first rem-args)) (processed-args (rest rem-args)
                                                                 (assoc processed-input
                                                                   :log true))
    (contains? #{"-p" "--pretty-print"} (first rem-args)) (processed-args (rest rem-args)
                                                                          (assoc processed-input
                                                                            :pretty-print true))
    (contains? #{"-f" "--output-file"} (first rem-args)) (processed-args (drop 2 rem-args)
                                                                         (assoc processed-input
                                                                           :output-file (second rem-args)))
    (contains? #{"-w" "--run-web-server"} (first rem-args)) (processed-args (rest rem-args)
                                                                            (assoc processed-input
                                                                              :web-server true))
    (empty? rem-args) processed-input
    :else (assoc processed-input :input-file (first rem-args))))

;; TODO [IMPROVE] if cli receives any other -[string] argument throw [option not available] exception and ask user to input the options again
;; in the current implementation an argument that is not an option will always be interpreted as a potential input file value

(s/fdef processed-args
        :args (s/cat :rem-args (s/coll-of string?)
                     :processed-input map?)
        :ret map?)

(defn print-job-queue-to-console!
  [job-queue]
  (pp/pprint job-queue))

(s/fdef print-job-queue-to-console!
        :args (s/cat :job-queue coll?))

(defn log-event-res-and-jqs-to-cli-and-return-res!
  [event res db]
  (log/info "adding event: " event)
  (log/info "resulting event: " res)
  (log/info "resulting jqs: " db)
  res)

(s/fdef log-event-res-and-jqs-to-cli-and-return-res!
        :args (s/cat :event ::specs.events/event
                     :res any?
                     :db ::specs.db/db)
        :ret any?
        :fn #(= (-> % :args :res)
                (-> % :ret)))