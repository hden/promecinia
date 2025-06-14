(defproject com.github.hden/promecinia "0.1.1"
  :description "Use [promesa promises](https://github.com/funcool/promesa) as lacinia ResolverResults."
  :url "https://github.com/hden/promecinia"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :managed-dependencies [[com.walmartlabs/lacinia "1.2.2"]
                         [funcool/promesa "11.0.678"]]
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [com.walmartlabs/lacinia]
                 [funcool/promesa]]
  :repl-options {:init-ns promecinia.core}
  :profiles
  {:dev {:dependencies [[datascript "1.7.5"]
                        [io.aviso/pretty "1.4.4"]
                        [superlifter "0.1.5"]]}})
