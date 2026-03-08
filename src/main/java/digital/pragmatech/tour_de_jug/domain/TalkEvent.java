package digital.pragmatech.tour_de_jug.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "talk_event")
public class TalkEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id", nullable = false)
    private AppUser speaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jug_id", nullable = false)
    private JavaUserGroup jug;

    @Column(name = "talk_title", nullable = false)
    private String talkTitle;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "slides_url")
    private String slidesUrl;

    @Column(name = "recording_url")
    private String recordingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TalkStatus status = TalkStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public TalkEvent() {}

    public UUID getId() { return id; }
    public AppUser getSpeaker() { return speaker; }
    public void setSpeaker(AppUser speaker) { this.speaker = speaker; }
    public JavaUserGroup getJug() { return jug; }
    public void setJug(JavaUserGroup jug) { this.jug = jug; }
    public String getTalkTitle() { return talkTitle; }
    public void setTalkTitle(String talkTitle) { this.talkTitle = talkTitle; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getSlidesUrl() { return slidesUrl; }
    public void setSlidesUrl(String slidesUrl) { this.slidesUrl = slidesUrl; }
    public String getRecordingUrl() { return recordingUrl; }
    public void setRecordingUrl(String recordingUrl) { this.recordingUrl = recordingUrl; }
    public TalkStatus getStatus() { return status; }
    public void setStatus(TalkStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
