package com.artemiy.player.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Songs credited to several artists at once ("Ado & Eve", "A, B feat. C") are listed under each of
 * them — the files themselves are left exactly as they are, this only changes how the app reads
 * the artist field. Band names that merely contain those characters ("Earth, Wind & Fire", "AC/DC")
 * are kept whole: a built-in list, plus the user's own additions from Settings → Общие.
 */
object ArtistNames {
    /** The user's own "don't split these" names (Settings), kept up to date by the settings screen. */
    var userKept by mutableStateOf<Set<String>>(emptySet())

    /**
     * Bumped whenever the answers below may have changed (the library was read, or the user's list
     * changed) — screens key their artist lists on it.
     */
    var version by mutableIntStateOf(0)
        private set

    /** The spelling shown for each artist, by [key]: the one used most in the library. */
    private var canonical: Map<String, String> = emptyMap()

    private val cache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()
    private var cacheFor: Set<String> = emptySet()

    /**
     * Every artist in [raw], in order, each once — spelled the way the library mostly spells them,
     * so "Eve" and "EVE" on different songs are the same artist.
     */
    fun split(raw: String): List<String> {
        val kept = userKept
        if (kept !== cacheFor) {
            cache.clear()
            cacheFor = kept
        }
        return cache.getOrPut(raw) {
            doSplit(raw, kept).map { canonical[key(it)] ?: it }.distinctBy { key(it) }
        }
    }

    /**
     * How two spellings of one artist are matched: ignoring case, extra spaces and full-width
     * forms (Japanese "ＥＶＥ" is "EVE").
     */
    fun key(name: String): String =
        java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKC).trim().replace(WHITESPACE, " ").lowercase()

    /** Reads the whole library once to pick each artist's usual spelling. Call when it (re)loads. */
    fun learn(songs: List<Song>) {
        val kept = userKept
        val spellings = HashMap<String, HashMap<String, Int>>()
        for (song in songs) {
            for (name in doSplit(song.artist, kept)) {
                spellings.getOrPut(key(name)) { HashMap() }.merge(name, 1, Int::plus)
            }
        }
        canonical = spellings.mapValues { (_, counts) -> counts.maxBy { it.value }.key }
        cache.clear()
        version++
    }

    /**
     * [songs] with every artist line and album name spelled the way most of the library spells it
     * ("twenty One Pilots" on one song of "Vessel" becomes "twenty one pilots", like the rest), the
     * file's own spelling kept in [Song.tagArtist] / [Song.tagAlbum].
     */
    fun withUsualSpellings(songs: List<Song>): List<Song> {
        fun usual(values: List<String>): Map<String, String> =
            values.groupBy { key(it) }.mapValues { (_, all) -> all.groupingBy { it }.eachCount().maxBy { it.value }.key }
        val artists = usual(songs.map { it.artist })
        val albums = usual(songs.map { it.album })
        return songs.map { song ->
            val artist = artists[key(song.artist)] ?: song.artist
            val album = albums[key(song.album)] ?: song.album
            if (artist == song.artist && album == song.album) song
            else song.copy(artist = artist, album = album, tagArtist = song.artist, tagAlbum = song.album)
        }
    }

    private val WHITESPACE = Regex("\\s+")

    private fun doSplit(raw: String, userKept: Set<String>): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || !SEPARATOR.containsMatchIn(trimmed)) return listOf(trimmed.ifEmpty { raw })
        // Protected names are swapped for placeholders first, so the separators inside them
        // don't count; then put back after splitting.
        var work = trimmed
        val protected = mutableListOf<String>()
        for (name in (BUILT_IN_SORTED + userKept.sortedByDescending { it.length })) {
            if (name.isBlank()) continue
            var index = work.indexOf(name, ignoreCase = true)
            while (index >= 0) {
                val original = work.substring(index, index + name.length)
                val token = "\u0001${protected.size}\u0002"
                protected += original
                work = work.substring(0, index) + token + work.substring(index + name.length)
                index = work.indexOf(name, startIndex = index + token.length, ignoreCase = true)
            }
        }
        val parts = work.split(SEPARATOR)
            .map { part -> PLACEHOLDER.replace(part) { protected[it.groupValues[1].toInt()] }.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        return parts.ifEmpty { listOf(trimmed) }
    }

    /**
     * What joins several artists: commas, semicolons, "&", the Japanese "、", "feat."/"ft."/
     * "featuring", "vs", a lowercase " x " or "×" between names, and a slash with spaces around it
     * (a bare "/" stays — "AC/DC").
     */
    private val SEPARATOR = Regex(
        """\s*(?:,|;|、|&|＆|\s(?:(?i:feat\.?|ft\.?|featuring|vs\.?))\s|\sx\s|\s?×\s?|\s/\s)\s*""",
    )
    private val PLACEHOLDER = Regex("\u0001(\\d+)\u0002")

    private val BUILT_IN_SORTED by lazy { BUILT_IN.sortedByDescending { it.length } }

    /** Names that contain a separator but are one artist. Matched ignoring case. */
    private val BUILT_IN = listOf(
        "Earth, Wind & Fire", "Earth Wind & Fire", "AC/DC", "Simon & Garfunkel", "Hall & Oates",
        "Daryl Hall & John Oates", "Crosby, Stills, Nash & Young", "Crosby, Stills & Nash", "Crosby & Nash",
        "Emerson, Lake & Palmer", "Emerson Lake & Palmer", "Blood, Sweat & Tears", "Peter, Paul and Mary",
        "Peter, Paul & Mary", "The Mamas & the Papas", "Mamas & Papas", "Sonny & Cher", "Captain & Tennille",
        "Seals & Crofts", "Loggins & Messina", "Zager & Evans", "Jan & Dean", "Chad & Jeremy",
        "Peter & Gordon", "Sam & Dave", "Ike & Tina Turner", "Ashford & Simpson", "Peaches & Herb",
        "Kool & the Gang", "Sly & the Family Stone", "Bob Marley & The Wailers", "Bob Marley & the Wailers",
        "Tom Petty & The Heartbreakers", "Huey Lewis & The News", "Echo & the Bunnymen",
        "Siouxsie & the Banshees", "Hootie & the Blowfish", "Prince & The Revolution",
        "Gerry & the Pacemakers", "Martha & the Vandellas", "Smokey Robinson & the Miracles",
        "Diana Ross & the Supremes", "Booker T. & the M.G.'s", "Booker T. & the MG's", "Derek & the Dominos",
        "Bob Seger & the Silver Bullet Band", "Joan Jett & the Blackhearts", "Nick Cave & the Bad Seeds",
        "Elvis Costello & the Attractions", "Katrina & the Waves", "Bruce Springsteen & the E Street Band",
        "Kenny Rogers & The First Edition", "Mike + The Mechanics", "Mike & the Mechanics",
        "Frankie Valli & the Four Seasons", "Paul Revere & the Raiders", "Tommy James & the Shondells",
        "Gladys Knight & the Pips", "Harold Melvin & the Blue Notes", "KC & the Sunshine Band",
        "Adam & the Ants", "Florence + The Machine", "Mumford & Sons", "Belle & Sebastian", "Iron & Wine",
        "Marina & The Diamonds", "Edward Sharpe & The Magnetic Zeros", "Tegan & Sara", "Brooks & Dunn",
        "Big & Rich", "Dan + Shay", "Maddie & Tae", "Matt & Kim", "Chase & Status", "Above & Beyond",
        "Aly & AJ", "Aly & Fila", "Nico & Vinz", "Dimitri Vegas & Like Mike", "Axwell & Ingrosso",
        "Axwell /\\ Ingrosso", "Macklemore & Ryan Lewis", "Eric B. & Rakim",
        "DJ Jazzy Jeff & The Fresh Prince", "Rob Base & DJ E-Z Rock", "Tyler, The Creator",
        "Tyler, the Creator", "Earth, Wind and Fire", "Crosby, Stills and Nash", "Kid Creole & the Coconuts",
        "Captain Beefheart & His Magic Band", "Ian Dury & the Blockheads",
        "Grandmaster Flash & the Furious Five", "Earth & Fire", "Cheech & Chong", "Hudson & Landry",
        "Death & Vanilla", "Of Mice & Men", "Heaven & Hell", "Jesus & Mary Chain", "The Jesus & Mary Chain",
        "Чиж & Co", "Earth, Wind & Fire & The Emotions", "Sly & Robbie", "Toots & the Maytals",
        "Desmond Dekker & the Aces", "Chaka Demus & Pliers", "Althea & Donna", "Womack & Womack",
        "Rufus & Chaka Khan", "Rufus featuring Chaka Khan", "Maze featuring Frankie Beverly",
        "Ray Parker Jr. & Raydio", "McFadden & Whitehead", "Delaney & Bonnie", "Richard & Linda Thompson",
        "Ian & Sylvia", "Mickey & Sylvia", "Bill Haley & His Comets", "Buddy Holly & the Crickets",
        "Dion & the Belmonts", "Little Anthony & the Imperials", "Frankie Lymon & the Teenagers",
        "Jay & the Americans", "Gary Lewis & the Playboys", "Freddie & the Dreamers",
        "Wayne Fontana & the Mindbenders", "Billy J. Kramer & the Dakotas", "Eric Burdon & the Animals",
        "Eric Burdon & War", "Country Joe & the Fish", "Big Brother & the Holding Company",
        "Mitch Ryder & the Detroit Wheels", "Gary Puckett & the Union Gap", "Sam the Sham & the Pharaohs",
        "? & the Mysterians", "Question Mark & the Mysterians", "Hank Ballard & the Midnighters",
        "James Brown & the Famous Flames", "Maurice Williams & the Zodiacs", "Johnny & the Hurricanes",
        "Joey Dee & the Starliters", "Dave Dee, Dozy, Beaky, Mick & Tich", "Martha & the Muffins",
        "Dr. Hook & the Medicine Show", "England Dan & John Ford Coley", "Brewer & Shipley", "Chas & Dave",
        "Peters & Lee", "Robson & Jerome", "Jon & Vangelis", "Nina & Frederik", "Esther & Abi Ofarim",
        "Renée & Renato", "Brian & Michael", "Hue & Cry", "Page & Plant", "Beck, Bogert & Appice",
        "Emerson, Lake & Powell", "Anderson, Bruford, Wakeman, Howe", "Nathaniel Rateliff & the Night Sweats",
        "Sharon Jones & the Dap-Kings", "Charles Bradley & His Extraordinaires",
        "Jason Isbell & the 400 Unit", "Grace Potter & the Nocturnals", "Fitz & the Tantrums",
        "Noah & the Whale", "Ezra Furman & the Harpoons", "Shovels & Rope", "She & Him", "Boy & Bear",
        "Angus & Julia Stone", "Joey + Rory", "Pete Rock & CL Smooth", "Heavy D & the Boyz",
        "Doug E. Fresh & the Get Fresh Crew", "Afrika Bambaataa & the Soulsonic Force", "Timbaland & Magoo",
        "Camo & Krooked", "Sunnery James & Ryan Marciano", "Lucas & Steve", "W&W", "Gabriel & Dresden",
        "Drumsound & Bassline Smith", "Super Junior-D&E", "Chage & Aska", "Tackey & Tsubasa", "Artik & Asti",
        "Florence & the Machine", "Prince & the New Power Generation", "Elvis Costello & the Imposters",
        "Bruce Hornsby & the Range", "John Cafferty & the Beaver Brown Band",
        "Southside Johnny & the Asbury Jukes", "Jim Kweskin & the Jug Band",
        "Stevie Ray Vaughan & Double Trouble", "Edie Brickell & New Bohemians",
        "Captain Sensible & the Softies", "Martha Reeves & the Vandellas", "Junior Walker & the All Stars",
        "Jr. Walker & the All Stars", "Crosby, Stills, Nash and Young",
    )
}

/** This song's artists, one per credited name (see [ArtistNames]). */
fun Song.artists(): List<String> = ArtistNames.split(artist)
