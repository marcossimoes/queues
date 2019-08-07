(ns queues.cli)

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