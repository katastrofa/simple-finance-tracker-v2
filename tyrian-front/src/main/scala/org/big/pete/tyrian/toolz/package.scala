package org.big.pete.tyrian

package object toolz {
  def createPermalink(full: String): String =
    full.trim.toLowerCase
      .replaceAll("\\s+", "-")
      .replaceAll("[^a-z0-9_-]+", "_")
}
