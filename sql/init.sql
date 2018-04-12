DROP TABLE IF EXISTS conversations CASCADE;
CREATE TABLE conversations (
	id SERIAL PRIMARY KEY,
        creation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        origin_url TEXT UNIQUE,
        full_text TEXT,
        other_info JSONB);
