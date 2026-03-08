CREATE TABLE app_user (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  github_id    BIGINT NOT NULL UNIQUE,
  username     VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  avatar_url   TEXT,
  email        VARCHAR(255),
  signed_up_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE java_user_group (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slug             VARCHAR(255) NOT NULL UNIQUE,
  name             VARCHAR(255) NOT NULL,
  description      TEXT,
  homepage_url     TEXT,
  social_url       TEXT,
  meetup_url       TEXT,
  latitude         DOUBLE PRECISION,
  longitude        DOUBLE PRECISION,
  country          VARCHAR(100),
  city             VARCHAR(100),
  established_year INT,
  contact_info     TEXT,
  deleted_at       TIMESTAMPTZ,
  inactive_since   TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE jug_admin (
  user_id UUID NOT NULL REFERENCES app_user(id),
  jug_id  UUID NOT NULL REFERENCES java_user_group(id),
  PRIMARY KEY (user_id, jug_id)
);

CREATE TABLE talk_event (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  speaker_id     UUID NOT NULL REFERENCES app_user(id),
  jug_id         UUID NOT NULL REFERENCES java_user_group(id),
  talk_title     VARCHAR(500) NOT NULL,
  event_time     TIMESTAMPTZ NOT NULL,
  slides_url     TEXT,
  recording_url  TEXT,
  status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
