package com.example.ebookreader.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.ebookreader.model.AudioTrack;
import com.example.ebookreader.model.Book;
import com.example.ebookreader.model.BookAvailability;
import com.example.ebookreader.model.BookContentBundle;
import com.example.ebookreader.model.Chapter;
import com.example.ebookreader.model.Genre;
import com.example.ebookreader.repository.AudioTrackRepository;
import com.example.ebookreader.repository.BookContentBundleRepository;
import com.example.ebookreader.repository.BookRepository;
import com.example.ebookreader.repository.ChapterRepository;
import com.example.ebookreader.repository.GenreRepository;

@Component
public class SupplementalCatalogSeeder implements CommandLineRunner {
    private static final String SOURCE_LIBRIVOX = "LIBRIVOX_PUBLIC_DOMAIN_AUDIO";
    private static final String SOURCE_ADEBIPORTAL = "ADEBIPORTAL_PUBLIC_AUDIO";
    private static final String SOURCE_CURATED_TEXT = "DIPLOMA_CURATED_TEXT";

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AudioTrackRepository audioTrackRepository;
    private final ChapterRepository chapterRepository;
    private final BookContentBundleRepository bundleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public SupplementalCatalogSeeder(
            BookRepository bookRepository,
            GenreRepository genreRepository,
            AudioTrackRepository audioTrackRepository,
            ChapterRepository chapterRepository,
            BookContentBundleRepository bundleRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${ebookreader.demo.seed-supplemental-catalog:true}") boolean enabled) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.audioTrackRepository = audioTrackRepository;
        this.chapterRepository = chapterRepository;
        this.bundleRepository = bundleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed();
    }

    @Transactional
    public void seed() {
        if (!enabled) {
            return;
        }
        AUDIOBOOKS.forEach(this::seedAudioBook);
        removeUnsupportedRussianRemoteAudio();
        READABLE_BOOKS.forEach(this::seedReadableBook);
        mergeAbaiQaraSozderRecords();
    }

    private void seedAudioBook(AudioBookSeed seed) {
        Book book = upsertBook(seed.goodreadsId(), seed.title(), seed.author(), seed.description(),
                seed.coverUrl(), seed.externalUrl(), seed.language(), seed.pageCount(), BookAvailability.AUDIO);
        attachGenres(book, seed.genres());
        ensureTracks(book, seed.tracks());
        updateAvailability(book);
    }

    private void seedReadableBook(ReadableBookSeed seed) {
        Book book = upsertBook(seed.goodreadsId(), seed.title(), seed.author(), seed.description(),
                seed.coverUrl(), seed.externalUrl(), seed.language(), seed.pageCount(), BookAvailability.TEXT);
        attachGenres(book, seed.genres());
        ensureChapters(book, seed.goodreadsId(), seed.sourceName(), seed.language(), seed.externalUrl(), seed.chapters());
        updateAvailability(book);
    }

    private void ensureTracks(Book book, List<TrackSeed> tracks) {
        Map<Integer, AudioTrack> existingByOrder = new HashMap<>();
        for (AudioTrack track : audioTrackRepository.findByBookIdOrderBySegmentOrderAsc(book.getId())) {
            existingByOrder.putIfAbsent(track.getSegmentOrder(), track);
        }

        for (TrackSeed trackSeed : tracks) {
            int order = Math.max(1, trackSeed.order());
            AudioTrack track = existingByOrder.get(order);
            if (track == null) {
                track = new AudioTrack();
                track.setBook(book);
                track.setSegmentOrder(order);
                track.setCreatedAt(LocalDateTime.now());
            }
            track.setTitle(trackSeed.title());
            track.setAudioPath(trackSeed.url());
            track.setOriginalFileName(fileName(trackSeed.url()));
            track.setContentType("audio/mpeg");
            track.setDurationMs(trackSeed.durationMs());
            audioTrackRepository.save(track);
        }
    }

    private void ensureChapters(
            Book book,
            String originalFileName,
            String sourceName,
            String language,
            String externalUrl,
            List<TextChapterSeed> chapters) {
        Map<Integer, Chapter> existingByOrder = new HashMap<>();
        for (Chapter chapter : chapterRepository.findByBookIdOrderByChapterOrderAsc(book.getId())) {
            existingByOrder.putIfAbsent(chapter.getChapterOrder(), chapter);
        }

        BookContentBundle savedBundle = null;
        for (int index = 0; index < chapters.size(); index++) {
            int order = index + 1;
            TextChapterSeed chapterSeed = chapters.get(index);
            Chapter chapter = existingByOrder.get(order);
            if (chapter == null) {
                if (savedBundle == null) {
                    savedBundle = createContentBundle(book, originalFileName, sourceName, language);
                }
                chapter = new Chapter();
                chapter.setBook(book);
                chapter.setChapterOrder(order);
                chapter.setContentBundle(savedBundle);
            }
            chapter.setTitle(chapterSeed.title());
            chapter.setContent(chapterSeed.content());
            chapter.setSourceType(SOURCE_CURATED_TEXT);
            chapter.setSourceHref(externalUrl);
            chapterRepository.save(chapter);
        }
    }

    private BookContentBundle createContentBundle(
            Book book,
            String originalFileName,
            String sourceName,
            String language) {
        BookContentBundle bundle = new BookContentBundle();
        bundle.setBook(book);
        bundle.setSourceType(SOURCE_CURATED_TEXT);
        bundle.setSourceName(sourceName);
        bundle.setOriginalFileName(originalFileName);
        bundle.setLanguageCode(language);
        return bundleRepository.save(bundle);
    }

    private void mergeAbaiQaraSozderRecords() {
        bookRepository.findByGoodreadsId("kz-abai-qara-sozder-readable").ifPresent(canonical -> {
            ensureTracks(canonical, ABAI_QARA_SOZDER_TRACKS);
            updateAvailability(canonical);

            bookRepository.findByGoodreadsId("ap-kz-abai-qara-sozder").ifPresent(duplicate -> {
                if (canonical.getId().equals(duplicate.getId())) {
                    return;
                }
                mergeDuplicateBook(canonical.getId(), duplicate.getId());
                updateAvailability(canonical);
            });
        });
    }

    private void removeUnsupportedRussianRemoteAudio() {
        for (String goodreadsId : List.of("lv-559", "lv-210", "lv-2145")) {
            bookRepository.findByGoodreadsId(goodreadsId).ifPresent(book -> {
                audioTrackRepository.deleteByBookId(book.getId());
                updateAvailability(book);
            });
        }
    }

    private void mergeDuplicateBook(Long canonicalBookId, Long duplicateBookId) {
        jdbcTemplate.update("""
                INSERT INTO book_genres (book_id, genre_id)
                SELECT ?, genre_id
                FROM book_genres
                WHERE book_id = ?
                ON CONFLICT DO NOTHING
                """, canonicalBookId, duplicateBookId);

        jdbcTemplate.update("UPDATE book_annotations SET book_id = ? WHERE book_id = ?",
                canonicalBookId, duplicateBookId);

        jdbcTemplate.update("""
                UPDATE book_review_replies reply
                SET review_user_book_id = canonical.id
                FROM user_books duplicate
                JOIN user_books canonical
                  ON canonical.user_id = duplicate.user_id
                 AND canonical.book_id = ?
                WHERE duplicate.book_id = ?
                  AND reply.review_user_book_id = duplicate.id
                """, canonicalBookId, duplicateBookId);

        jdbcTemplate.update("""
                UPDATE user_books canonical
                SET bookmarked = canonical.bookmarked OR duplicate.bookmarked,
                    hidden_from_library = canonical.hidden_from_library AND duplicate.hidden_from_library,
                    current_chapter = CASE
                        WHEN duplicate.last_read_at IS NOT NULL
                         AND (canonical.last_read_at IS NULL OR duplicate.last_read_at > canonical.last_read_at)
                        THEN duplicate.current_chapter ELSE canonical.current_chapter END,
                    segment_order = CASE
                        WHEN duplicate.last_read_at IS NOT NULL
                         AND (canonical.last_read_at IS NULL OR duplicate.last_read_at > canonical.last_read_at)
                        THEN duplicate.segment_order ELSE canonical.segment_order END,
                    segment_progress = CASE
                        WHEN duplicate.last_read_at IS NOT NULL
                         AND (canonical.last_read_at IS NULL OR duplicate.last_read_at > canonical.last_read_at)
                        THEN duplicate.segment_progress ELSE canonical.segment_progress END,
                    audio_position_ms = CASE
                        WHEN duplicate.last_read_at IS NOT NULL
                         AND (canonical.last_read_at IS NULL OR duplicate.last_read_at > canonical.last_read_at)
                        THEN duplicate.audio_position_ms ELSE canonical.audio_position_ms END,
                    last_mode = CASE
                        WHEN duplicate.last_read_at IS NOT NULL
                         AND (canonical.last_read_at IS NULL OR duplicate.last_read_at > canonical.last_read_at)
                        THEN duplicate.last_mode ELSE canonical.last_mode END,
                    status = CASE
                        WHEN duplicate.last_read_at IS NOT NULL
                         AND (canonical.last_read_at IS NULL OR duplicate.last_read_at > canonical.last_read_at)
                        THEN duplicate.status ELSE canonical.status END,
                    started_at = CASE
                        WHEN canonical.started_at IS NULL THEN duplicate.started_at
                        WHEN duplicate.started_at IS NULL THEN canonical.started_at
                        WHEN duplicate.started_at < canonical.started_at THEN duplicate.started_at
                        ELSE canonical.started_at END,
                    finished_at = COALESCE(canonical.finished_at, duplicate.finished_at),
                    last_read_at = CASE
                        WHEN canonical.last_read_at IS NULL THEN duplicate.last_read_at
                        WHEN duplicate.last_read_at IS NULL THEN canonical.last_read_at
                        WHEN duplicate.last_read_at > canonical.last_read_at THEN duplicate.last_read_at
                        ELSE canonical.last_read_at END,
                    rating = COALESCE(duplicate.rating, canonical.rating),
                    rated_at = CASE
                        WHEN canonical.rated_at IS NULL THEN duplicate.rated_at
                        WHEN duplicate.rated_at IS NULL THEN canonical.rated_at
                        WHEN duplicate.rated_at > canonical.rated_at THEN duplicate.rated_at
                        ELSE canonical.rated_at END,
                    review_text = CASE
                        WHEN duplicate.review_updated_at IS NOT NULL
                         AND (canonical.review_updated_at IS NULL OR duplicate.review_updated_at > canonical.review_updated_at)
                        THEN duplicate.review_text ELSE canonical.review_text END,
                    review_created_at = CASE
                        WHEN canonical.review_created_at IS NULL THEN duplicate.review_created_at
                        WHEN duplicate.review_created_at IS NULL THEN canonical.review_created_at
                        WHEN duplicate.review_created_at < canonical.review_created_at THEN duplicate.review_created_at
                        ELSE canonical.review_created_at END,
                    review_updated_at = CASE
                        WHEN canonical.review_updated_at IS NULL THEN duplicate.review_updated_at
                        WHEN duplicate.review_updated_at IS NULL THEN canonical.review_updated_at
                        WHEN duplicate.review_updated_at > canonical.review_updated_at THEN duplicate.review_updated_at
                        ELSE canonical.review_updated_at END
                FROM user_books duplicate
                WHERE canonical.user_id = duplicate.user_id
                  AND canonical.book_id = ?
                  AND duplicate.book_id = ?
                """, canonicalBookId, duplicateBookId);

        jdbcTemplate.update("""
                DELETE FROM user_books duplicate
                USING user_books canonical
                WHERE duplicate.user_id = canonical.user_id
                  AND duplicate.book_id = ?
                  AND canonical.book_id = ?
                """, duplicateBookId, canonicalBookId);

        jdbcTemplate.update("UPDATE user_books SET book_id = ? WHERE book_id = ?",
                canonicalBookId, duplicateBookId);
        jdbcTemplate.update("DELETE FROM audio_tracks WHERE book_id = ?", duplicateBookId);
        jdbcTemplate.update("DELETE FROM chapters WHERE book_id = ?", duplicateBookId);
        jdbcTemplate.update("DELETE FROM book_content_bundles WHERE book_id = ?", duplicateBookId);
        jdbcTemplate.update("DELETE FROM book_genres WHERE book_id = ?", duplicateBookId);
        jdbcTemplate.update("DELETE FROM books WHERE id = ?", duplicateBookId);
    }

    private Book upsertBook(
            String goodreadsId,
            String title,
            String author,
            String description,
            String coverUrl,
            String externalUrl,
            String language,
            Integer pageCount,
            BookAvailability fallbackAvailability) {
        Book book = bookRepository.findByGoodreadsId(goodreadsId).orElseGet(Book::new);
        book.setGoodreadsId(goodreadsId);
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setCoverUrl(coverUrl);
        book.setExternalUrl(externalUrl);
        book.setLanguageCode(language);
        book.setPageCount(pageCount);
        if (book.getAverageRating() == null) {
            book.setAverageRating(4.0);
        }
        if (book.getRatingsCount() == null) {
            book.setRatingsCount(0);
        }
        if (book.getReviewCount() == null) {
            book.setReviewCount(0);
        }
        if (book.getAvailability() == BookAvailability.METADATA_ONLY) {
            book.setAvailability(fallbackAvailability);
        }
        return bookRepository.save(book);
    }

    private void attachGenres(Book book, Set<String> genres) {
        for (String genreName : genres) {
            Genre genre = genreRepository.findByName(genreName)
                    .orElseGet(() -> genreRepository.save(new Genre(genreName)));
            book.getGenres().add(genre);
        }
        bookRepository.save(book);
    }

    private void updateAvailability(Book book) {
        boolean hasText = chapterRepository.countByBookId(book.getId()) > 0;
        boolean hasAudio = audioTrackRepository.countByBookId(book.getId()) > 0;
        if (hasText && hasAudio) {
            book.setAvailability(BookAvailability.SYNCED);
        } else if (hasText) {
            book.setAvailability(BookAvailability.TEXT);
        } else if (hasAudio) {
            book.setAvailability(BookAvailability.AUDIO);
        } else {
            book.setAvailability(BookAvailability.METADATA_ONLY);
        }
        bookRepository.save(book);
    }

    private String fileName(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private record AudioBookSeed(
            String goodreadsId,
            String title,
            String author,
            String description,
            String coverUrl,
            String externalUrl,
            String language,
            Integer pageCount,
            Set<String> genres,
            List<TrackSeed> tracks) {
    }

    private record TrackSeed(int order, String title, String url, Long durationMs) {
    }

    private record ReadableBookSeed(
            String goodreadsId,
            String title,
            String author,
            String description,
            String coverUrl,
            String externalUrl,
            String language,
            Integer pageCount,
            Set<String> genres,
            String sourceName,
            List<TextChapterSeed> chapters) {
    }

    private record TextChapterSeed(String title, String content) {
    }

    private static AudioBookSeed lv(String id, String title, String author, String lang, String url, String trackTitle, String audioUrl, long durationMs) {
        return new AudioBookSeed(
                "lv-" + id,
                title,
                author,
                "Public-domain audiobook from LibriVox, seeded for the diploma demo catalog.",
                "",
                url,
                lang,
                null,
                Set.of("audiobook", "public domain", "classics"),
                List.of(new TrackSeed(1, trackTitle, audioUrl, durationMs)));
    }

    private static final List<TrackSeed> ABAI_QARA_SOZDER_TRACKS = List.of(
            new TrackSeed(1, "Қара сөздер - 1 сөз", "https://adebiportal.kz/storage/upload/1/2017/11/21/7a6666dc6aa3e10fdc54179c38b7bf16.mp3", null),
            new TrackSeed(2, "Қара сөздер - 2 сөз", "https://adebiportal.kz/storage/upload/1/2017/11/21/b0796a6ca47d2bd9798e7b1469c0f87b.mp3", null),
            new TrackSeed(3, "Қара сөздер - 3 сөз", "https://adebiportal.kz/storage/upload/1/2017/11/21/00f2a0d7d6f59b040090a298300eabd8.mp3", null));

    private static final List<TextChapterSeed> ABAI_QARA_SOZDER_CHAPTERS = List.of(
            new TextChapterSeed("Бірінші сөз", """
                    Бұл жасқа келгенше жақсы өткіздік пе, жаман өткіздік пе, әйтеуір бірталай өмірімізді өткіздік: алыстық, жұлыстық, айтыстық, тартыстық - әурешілікті көре-көре келдік. Енді жер ортасы жасқа келдік: қажыдық, жалықтық; қылып жүрген ісіміздің баянсызын, байлаусызын көрдік, бәрі қоршылық екенін білдік. Ал, енді қалған өмірімізді қайтіп, не қылып өткіземіз? Соны таба алмай өзім де қайранмын.

                    Ел бағу? Жоқ, елге бағым жоқ. Бағусыз дертке ұшырайын деген кісі бақпаса, не албыртқан, көңілі басылмаған жастар бағамын демесе, бізді құдай сақтасын!

                    Мал бағу? Жоқ, баға алмаймын. Балалар өздеріне керегінше өздері бағар. Енді қартайғанда қызығын өзің түгел көре алмайтұғын, ұры, залым, тілемсектердің азығын бағып беремін деп, қалған аз ғана өмірімді қор қылар жайым жоқ.

                    Ғылым бағу? Жоқ, ғылым бағарға да ғылым сөзін сөйлесер адам жоқ. Білгеніңді кімге үйретерсің, білмегеніңді кімнен сұрарсың? Елсіз-күнсізде кездемені жайып салып, қолына кезін алып отырғанның не пайдасы бар? Мұңдасып шер тарқатысар кісі болмаған соң, ғылым өзі - бір тез қартайтатұғын күйік.

                    Софылық қылып, дін бағу? Жоқ, ол да болмайды, оған да тыныштық керек. Не көңілде, не көрген күніңде бір тыныштық жоқ, осы елге, осы жерде не қылған софылық?

                    Балаларды бағу? Жоқ, баға алмаймын. Бағар едім, қалайша бағудың мәнісін де білмеймін, не болсын деп бағам, қай елге қосайын, қай харекетке қосайын? Балаларымның өзіне ілгері өмірінің, білімінің пайдасын тыныштықпенен керерлік орын тапқаным жоқ, қайда бар, не қыл дерімді біле алмай отырмын, не бол деп бағам? Оны да ермек қыла алмадым.

                    Ақыры ойладым: осы ойыма келген нәрселерді қағазға жаза берейін, ақ қағаз бен қара сияны ермек қылайын, кімде-кім ішінен керекті сөз тапса, жазып алсын, я оқысын, керегі жоқ десе, өз сөзім өзімдікі дедім де, ақыры осыған байладым, енді мұнан басқа ешбір жұмысым жоқ.
                    """),
            new TextChapterSeed("Екінші сөз", """
                    Мен бала күнімде естуші едім, біздің қазақ сартты көрсе, күлуші еді «енеңді ұрайын, кең қолтық, шүлдіреген тәжік, Арқадан үй төбесіне саламын деп, қамыс артқан, бұтадан қорыққан, көз көргенде «әке-үке» десіп, шығып кетсе, қызын боқтасқан, «сарт-сұрт деген осы» деп. Ноғайды көрсе, оны да боқтап күлуші еді: «түйеден қорыққан ноғай, атқа мінсе - шаршап, жаяу жүрсе - демін алады, ноғай дегенше, ноқай десеңші, түкке ыңғайы келмейді, солдат ноғай, қашқын ноғай, башалшік ноғай» деп.

                    Орысқа да күлуші еді: «ауылды көрсе шапқан, жаман сасыр бас орыс» деп.

                    Орыс ойына келгенін қылады деген... не айтса соған нанады, «ұзын құлақты тауып бер депті» деп.

                    Сонда мен ойлаушы едім: ей, құдай-ай, бізден басқа халықтың бәрі антұрған, жаман келеді екен, ең тәуір халық біз екенбіз деп, әлгі айтылмыш сөздерді бір үлкен қызық көріп, қуанып күлуші едім.

                    Енді қарап тұрсам, сарттың екпеген егіні жоқ, шығармаған жемісі жоқ, саудагерінің жүрмеген жері жоқ, қылмаған шеберлігі жоқ. Өзіменен өзі әуре болып, біріменен бірі ешбір шаһары жауласпайды! Орысқа қарамай тұрғанда қазақтың өлісінің ахиреттігін, тірісінің киімін сол жеткізіп тұрды. Әке балаға қимайтұғын малыңды кірелеп сол айдап кетіп тұрды ғой. Орысқа қараған соң да, орыстың өнерлерін бізден олар көп үйреніп кетті. Үлкен байлар да, үлкен молдалар да, ептілік, қырмызылық, сыпайылық - бәрі соларда.

                    Ноғайға қарасам, солдаттыққа да шыдайды, кедейлікке де шыдайды, қазаға да шыдайды, молда, медресе сақтап, дін күтуге де шыдайды. Еңбек қылып, мал табудың да жөнін солар біледі, салтанат, әсем де соларда. Оның малдыларына, құзғын тамағымыз үшін, біріміз жалшы, біріміз қош алушымыз. Біздің ең байымызды: «сәнің шақшы аяғың білән пышыратырға қойған идән түгіл, шық, сасық казақ», - деп үйінен қуып шығарады. Оның бәрі - бірін-бірі қуып қор болмай, шаруа қуып, өнер тауып, мал тауып, зор болғандық әсері.

                    Орысқа айтар сөз де жоқ, біз құлы, күңі құрлы да жоқпыз. Бағанағы мақтан, бағанағы қуанған, күлген сөздеріміз қайда?
                    """));

    private static final List<AudioBookSeed> AUDIOBOOKS = List.of(
            lv("47", "Count of Monte Cristo", "Alexandre Dumas", "eng", "https://librivox.org/the-count-of-monte-cristo-by-alexandre-dumas/", "Marseilles - The Arrival", "https://www.archive.org/download/count_monte_cristo_0711_librivox/count_of_monte_cristo_001_dumas_64kb.mp3", 1179000L),
            lv("52", "Letters of Two Brides", "Honore de Balzac", "eng", "https://librivox.org/letters-of-two-brides-by-honore-de-balzac/", "Letter 1", "https://www.archive.org/download/letters_brides_0709_librivox/letters_of_two_brides_01_debalzac_64kb.mp3", 1764000L),
            lv("53", "Bleak House", "Charles Dickens", "eng", "https://librivox.org/bleak-house-by-charles-dickens/", "In Chancery", "https://www.archive.org/download/bleak_house_cl_librivox/bleak_house_01_dickens_64kb.mp3", 1582000L),
            lv("54", "Penguin Island", "Anatole France", "eng", "https://librivox.org/penguin-island-by-anatole-france/", "Life of Saint Mael", "https://www.archive.org/download/penguin_island_ms_librivox/penguin_island_01_france_64kb.mp3", 210000L),
            lv("55", "This Side of Paradise", "F. Scott Fitzgerald", "eng", "https://librivox.org/this-side-of-paradise-by-f-scott-fitzgerald", "Book 1 Chapter 1 Part 1", "https://www.archive.org/download/this_side_paradise_librivox/thissideofparadise_01_fitzgerald_64kb.mp3", 1740000L),
            lv("56", "Secret Garden", "Frances Hodgson Burnett", "eng", "https://librivox.org/the-secret-garden-by-frances-hodgson-burnett/", "There is No One Left", "https://www.archive.org/download/secret_garden_librivox/secretgarden_01_burnett_64kb.mp3", 808000L),
            lv("57", "Twenty Years After", "Alexandre Dumas", "eng", "https://librivox.org/twenty-years-after-by-alexandre-dumas/", "The Shade of Cardinal Richelieu", "https://www.archive.org/download/twentyyearsafter_0904_librivox/twentyyearsafter_01_dumas_64kb.mp3", 1429000L),
            lv("59", "Adventures of Huckleberry Finn", "Mark Twain", "eng", "https://librivox.org/the-adventures-of-huckleberry-finn-by-mark-twain/", "Chapter 01", "https://www.archive.org/download/huck_finn_librivox/huckfinn_01_twain_apc_64kb.mp3", 532000L),
            lv("64", "Heart of Darkness", "Joseph Conrad", "eng", "https://librivox.org/heart-of-darkness-by-joseph-conrad", "Chapter 1 Part 1", "https://www.archive.org/download/heart_of_darkness/heart_of_darkness_1a_conrad_64kb.mp3", 2650000L),
            lv("65", "Odyssey", "Homer", "eng", "https://librivox.org/the-odyssey-by-homer/", "Book 01", "https://www.archive.org/download/odyssey_butler_librivox/odyssey_01_homer_butler_64kb.mp3", 1386000L),
            lv("67", "Divine Comedy", "Dante Alighieri", "eng", "https://librivox.org/the-divine-comedy-by-dante-alighieri/", "Inferno: Canto I - Canto V", "https://www.archive.org/download/divine_comedy_librivox/divinecomedy_longfellow_01_dante_64kb.mp3", 2590000L),
            lv("68", "Unbeaten Tracks in Japan", "Isabella L. Bird", "eng", "https://librivox.org/unbeaten-tracks-in-japan-by-isabella-l-bird/", "Preface", "https://www.archive.org/download/unbeaten_tracks_japan_ava_librivox/unbeatentracksjapan_00_bird_64kb.mp3", 391000L),
            lv("71", "Canterville Ghost", "Oscar Wilde", "eng", "https://librivox.org/the-canterville-ghost-by-oscar-wilde/", "Chapters 1 to 3", "https://www.archive.org/download/canterville_ghost_librivox/cantervilleghost_1-3_wilde_64kb.mp3", 2206000L),
            lv("73", "Fabulas de Esopo, Vol. 1", "Aesop", "spa", "https://librivox.org/las-fabulas-de-esopo-vol-01/", "El aguila, el cuervo y el pastor", "https://www.archive.org/download/fabulas_esopo_01_librivox/fabula_01_001_esopo_64kb.mp3", 106000L),
            lv("74", "Mother Goose in Prose", "L. Frank Baum", "eng", "https://librivox.org/mother-goose-in-prose-by-l-frank-baum/", "Introduction", "https://www.archive.org/download/mother_goose_prose_librivox/mother_goose_01_baum_64kb.mp3", 595000L),
            lv("75", "Uncle Tom's Cabin", "Harriet Beecher Stowe", "eng", "https://librivox.org/uncle-toms-cabin-by-harriet-beecher-stowe/", "Chapter 1", "https://www.archive.org/download/uncle_toms_cabin_librivox/uncletom_01_stowe_64kb.mp3", 1536000L),
            lv("76", "Truth About Jesus. Is He a Myth?", "M. M. Mangasarian", "eng", "https://librivox.org/the-truth-about-jesus-is-he-a-myth-by-m-m-mangasarian/", "A Parable", "https://www.archive.org/download/truth_about_jesus_librivox/jesus_myth_mangasarian_01_sc_64kb.mp3", 854000L),
            lv("78", "Foolish Dictionary", "Charles Wayland Towne", "eng", "https://librivox.org/the-foolish-dictionary-by-gideon-wurdz/", "Preface and Letter A", "https://www.archive.org/download/foolish_dictionary_librivox/thefoolishdictionary_01_wurdz_64kb.mp3", 412000L),
            lv("79", "Ranald Bannerman's Boyhood", "George MacDonald", "eng", "https://librivox.org/ranald-bannermans-boyhood-by-george-macdonald/", "Introductory", "https://www.archive.org/download/ranald_bannermans_boyhood_librivox/ranaldboyhood_01_georgemacdonald_64kb.mp3", 218000L),
            lv("80", "Fables de La Fontaine, livre 01", "Jean de La Fontaine", "fra", "https://librivox.org/fables_la_fontaine_01", "La Cigale et la Fourmi", "https://www.archive.org/download/fables_lafontaine_01_librivox/fables_01_01_lafontaine_64kb.mp3", 71000L),
            lv("81", "Dream Psychology", "Sigmund Freud", "eng", "https://librivox.org/dream-psychology-by-sigmund-freud/", "Introduction", "https://www.archive.org/download/dream_psychology_librivox/dreampsychology_00_sigmundfreud_64kb.mp3", 811000L),
            lv("82", "Winnetou I", "Karl May", "deu", "https://librivox.org/winnetou-i-by-karl-may/", "Einleitung", "https://www.archive.org/download/winnetou1_librivox/winnetou1_01_may_64kb.mp3", 644000L),
            lv("83", "Vindication of the Rights of Woman", "Mary Wollstonecraft", "eng", "https://librivox.org/a-vindication-of-the-rights-of-woman-by-mary-wollstonecraft/", "Brief Sketch of Mary Wollstonecraft", "https://www.archive.org/download/vindication_woman_librivox/vindication_00a_wollstonecraft_64kb.mp3", 968000L),
            lv("84", "Getting of Wisdom", "Henry Handel Richardson", "eng", "https://librivox.org/the-getting-of-wisdom-by-henry-handel-richardson/", "Chapter 1", "https://www.archive.org/download/getting_wisdom_librivox/getting_of_wisdom_01_richardson_64kb.mp3", 939000L),
            lv("85", "Consolation of Philosophy", "Boethius", "eng", "https://librivox.org/the-consolation-of-philosophy-by-boethius/", "Preface and Proem", "https://www.archive.org/download/the_consolation_of_philosophy_librivox/consolationphilosophy_01_boethius_64kb.mp3", 441000L),
            lv("86", "Emma", "Jane Austen", "eng", "https://librivox.org/emma-by-jane-austen-solo/", "Volume 1 Chapter 1", "https://www.archive.org/download/emma_solo_librivox/emma_01_01_austen_64kb.mp3", 1297000L),
            lv("88", "Mrs. Caudle's Curtain Lectures", "Douglas William Jerrold", "eng", "https://librivox.org/mrs-caudles-curtain-lectures-by-douglas-william-jerrold/", "Introduction", "https://www.archive.org/download/mrs_caudles_curtain_lectures_librivox/mrs_caudles_curtain_lectures_01_jerrold_64kb.mp3", 525000L),
            lv("89", "Fabulas de Esopo, Vol. 2", "Aesop", "spa", "https://librivox.org/fabulas-de-esopo-vol-02/", "Las Ranas y el Pantano Seco", "https://www.archive.org/download/fabulas_esopo_02_librivox/fabula_02_031_esopo_64kb.mp3", 73000L),
            lv("90", "Importance of Being Earnest", "Oscar Wilde", "eng", "https://librivox.org/the-importance-of-being-earnest-by-oscar-wilde/", "Act 1", "https://www.archive.org/download/being_earnest_librivox/Earnest_Act_1_64kb.mp3", 3046000L)
    );

    private static final List<ReadableBookSeed> READABLE_BOOKS = List.of(
            new ReadableBookSeed(
                    "kz-abai-qara-sozder-readable",
                    "Абайдың қара сөздері",
                    "Абай Құнанбайұлы",
                    "Қазақ әдебиетінің классикалық философиялық прозасы.",
                    "https://adebiportal.kz/storage/tmp/resize/audio/1200_0_01c3c89f1d871d6ae06d5f50501fff0d.jpg",
                    "https://kk.wikibooks.org/wiki/Абайдың_қара_сөздері",
                    "kaz",
                    120,
                    Set.of("kazakh literature", "philosophy", "public domain"),
                    "Kazakh public-domain reading selection",
                    ABAI_QARA_SOZDER_CHAPTERS),
            new ReadableBookSeed(
                    "kz-abai-olenderi",
                    "Абай өлеңдері",
                    "Абай Құнанбайұлы",
                    "Абайдың поэзиясынан қазақша оқылымға арналған қысқа таңдама.",
                    "",
                    "https://kk.wikisource.org/wiki/Абай_Құнанбайұлы",
                    "kaz",
                    90,
                    Set.of("kazakh literature", "poetry", "public domain"),
                    "Kazakh public-domain reading selection",
                    List.of(new TextChapterSeed("Таңдамалы өлеңдер", "Абай поэзиясы қазақ жазба әдебиетінің іргетасы саналады. Бұл каталог жазбасы қазақ тіліндегі оқу қорын кеңейту үшін қосылды."))),
            new ReadableBookSeed(
                    "ru-pg-detstvo",
                    "Детство",
                    "Leo Tolstoy",
                    "Russian public-domain text of Tolstoy's first published novel.",
                    "https://www.gutenberg.org/cache/epub/19681/pg19681.cover.medium.jpg",
                    "https://www.gutenberg.org/ebooks/19681",
                    "rus",
                    160,
                    Set.of("russian literature", "classics", "public domain"),
                    "Project Gutenberg public-domain text",
                    List.of(new TextChapterSeed("Readable source", "Public-domain Russian text available from Project Gutenberg. This seeded record expands the readable Russian catalog for the diploma demo."))),
            new ReadableBookSeed(
                    "ru-pg-derzhavin-ody",
                    "Духовные оды",
                    "Gavriil Derzhavin",
                    "Russian public-domain poetry by Gavriil Derzhavin.",
                    "https://www.gutenberg.org/cache/epub/14741/pg14741.cover.medium.jpg",
                    "https://www.gutenberg.org/ebooks/14741",
                    "rus",
                    80,
                    Set.of("russian literature", "poetry", "public domain"),
                    "Project Gutenberg public-domain text",
                    List.of(new TextChapterSeed("Readable source", "Public-domain Russian poetry available from Project Gutenberg. This seeded record expands the readable Russian catalog for the diploma demo."))),
            new ReadableBookSeed(
                    "ru-pg-pushkin-tabak",
                    "Красавице, которая нюхала табак",
                    "Alexander Pushkin",
                    "Russian public-domain poem by Alexander Pushkin.",
                    "https://www.gutenberg.org/cache/epub/5316/pg5316.cover.medium.jpg",
                    "https://www.gutenberg.org/ebooks/5316",
                    "rus",
                    20,
                    Set.of("russian literature", "poetry", "public domain"),
                    "Project Gutenberg public-domain text",
                    List.of(new TextChapterSeed("Readable source", "Public-domain Russian poem available from Project Gutenberg. This seeded record expands the readable Russian catalog for the diploma demo.")))
    );

}
