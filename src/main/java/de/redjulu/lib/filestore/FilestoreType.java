package de.redjulu.lib.filestore;

/**
 * Storage backend: network DB or local file DB. Configure in db.yml.
 */
public enum FilestoreType {
    H2, MYSQL, MARIADB, POSTGRES
}
