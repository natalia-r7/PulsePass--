CREATE TABLE venues (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    active BOOLEAN NOT NULL
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    minimum_age INTEGER,
    venue_id BIGINT NOT NULL,
    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id)
);

CREATE TABLE artists (
    id BIGSERIAL PRIMARY KEY,
    stage_name VARCHAR(150) NOT NULL UNIQUE,
    country VARCHAR(100),
    genre VARCHAR(100),
    active BOOLEAN NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL
);

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    city VARCHAR(100),
    birth_date DATE,
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL,
    purchase_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT fk_tickets_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_event
        FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE event_artists (
    event_id BIGINT NOT NULL,
    artist_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, artist_id),
    CONSTRAINT fk_event_artists_event
        FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_event_artists_artist
        FOREIGN KEY (artist_id) REFERENCES artists(id)
);

CREATE INDEX idx_events_venue_id
    ON events(venue_id);

CREATE INDEX idx_tickets_user_id
    ON tickets(user_id);

CREATE INDEX idx_tickets_event_id
    ON tickets(event_id);

CREATE INDEX idx_event_artists_artist_id
    ON event_artists(artist_id);