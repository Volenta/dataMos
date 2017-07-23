(ns datamos.core-vars
  "Contains the var atoms of datamos.core. Exists to ease reloading of source files, as to ease development.")

(def local-settings
  (atom {}))

(def config-queue-settings
  (atom {}))

(def register
  (atom {}))

(def global-config
  (atom {}))