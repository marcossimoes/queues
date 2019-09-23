(ns queues.core
  (:require [clojure.spec.alpha :as s]
            [queues.cli :as cli]
            [queues.controllers.batch-processor :as batch-processor]
            [queues.controllers.new-event-processor :as new-event-processor]
            [queues.init :as init]
            [queues.server :as server])
  (:gen-class))

(defn process-events-by-input-method
  [{:keys [web-server], :as options}]
  (cond
    web-server (server/start)
    :else (batch-processor/outputs-jobs-assigned-created-by-event-processor-from-file-to-output-options! new-event-processor/processed-event-with-log!
                                                                                                         ;; default option
                                                                                                         options)))

(s/fdef process-events-by-input-method
        :args (s/cat :options map?))

(defn -main
  [& args]
  (let [options (cli/processed-args args init/default-opts)
        {:keys [log]} options]
    ;; TODO [QUESTION; LOGGING] what is the best way to treat logging?
    (binding [init/*logging* log]
      (process-events-by-input-method options))))

;;TODO [QUESTION; ARCH] is making funcs private relevant as to document these functions are used outside this ns?
;;TODO [QUESTION; TEST] is it okay for -main to not have a s/fdef?
;;TODO [IMPROVE; SECURITY] refactor file reading to use buffer and edn
;;TODO [IMPROVE; FUNCTIONALITY] include time stamp in the beginning of output file name so if you run the program multiple times it does not overrides the previous output file
;;TODO [READ] review which functions should be !
;;TODO [READ] review unused references, let variables in tests, etc.
;;TODO [READ] requires in alphabetical order
;;TODO [EXCEPTIONS] implement error and exception handling in production and tests
;;TODO [READ] abbreviate logic and specs folders in variable names to lg and sp
;;TODO [READ] check for rotted/unused specs
;;TODO [READ] move common data in tests' lets to fixtures