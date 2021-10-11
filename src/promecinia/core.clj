(ns promecinia.core
  (:require [com.walmartlabs.lacinia.resolve :as lr]
            [promesa.core :as p])
  (:import [java.util.concurrent CompletableFuture]))

(defn show-stack-trace
  "Translate all internal errors to GraphQL errors. Don't do this in production."
  [x]
  (if (instance? Throwable x)
    (let [{:as error :keys [cause]} (Throwable->map x)]
      (lr/with-error nil (assoc error :message cause)))
    x))

(defn show-stack-traces
  "Translate all internal errors to GraphQL errors. Don't do this in production."
  [e]
  (let [nil-with-error (show-stack-trace e)]
    (repeatedly (constantly nil-with-error))))

(def ^:const internal-server-error {:message "Internal Server Error."})

(extend-protocol com.walmartlabs.lacinia.resolve/ResolverResult
  CompletableFuture
  (on-deliver! [promise callback]
    (p/handle promise
      (fn [value error]
        (let [result (cond-> value
                       error (lr/with-error internal-server-error))]
          (callback result))))
    promise))

(defn attach-context [promise context]
  (p/then promise #(lr/with-context % context)))

(defn attach-error [promise error]
  (p/then promise #(lr/with-error % error)))

(defn attach-warning [promise warning]
  (p/then promise #(lr/with-warning % warning)))

(defn attach [promise {:keys [error warning context]}]
  (cond-> promise
    context (attach-context context)
    error (attach-error error)
    warning (attach-warning warning)))
