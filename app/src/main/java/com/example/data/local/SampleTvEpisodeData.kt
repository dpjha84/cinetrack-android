package com.example.data.local

import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonItem
import com.example.data.model.TvWatchStatus

object SampleTvEpisodeData {

    fun getCuratedSeasons(media: MediaEntity): List<SeasonItem> {
        val seriesId = media.id
        val seriesTitle = media.title
        val seasonsCount = maxOf(1, media.totalSeasons)
        val poster = media.posterUrl
        val backdrop = media.backdropUrl

        return when (seriesId) {
            "tv_stranger_things" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                    episodeCount = 8,
                    releaseYear = 2016,
                    airDate = "2016-07-15",
                    rating = 8.6f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "It's 1984 and the citizens of Hawkins are still reeling from the horrors of the Demogorgon and the secrets of Hawkins Lab. But an even more sinister entity threatens those who survived.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                    episodeCount = 9,
                    releaseYear = 2017,
                    airDate = "2017-10-27",
                    rating = 8.5f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "It's 1985 in Hawkins, Indiana, and summer's heating up. School's out, there's a brand new mall in town, and the Hawkins crew are on the cusp of adulthood. Romance blossoms and endangers group dynamics.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/x2LSRK2Cm7MZhjluni1msVJ3wDF.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                    episodeCount = 8,
                    releaseYear = 2019,
                    airDate = "2019-07-04",
                    rating = 8.4f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 4,
                    name = "Season 4",
                    overview = "It's been six months since the Battle of Starcourt. Struggling with the aftermath, our group of friends are separated for the first time — and navigating the complexities of high school hasn't made things easier.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                    episodeCount = 9,
                    releaseYear = 2022,
                    airDate = "2022-05-27",
                    rating = 8.9f
                )
            )

            "tv_breaking_bad" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "Diagnosed with terminal lung cancer, chemistry teacher Walter White teams up with former student Jesse Pinkman to manufacture and sell crystal meth.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                    episodeCount = 7,
                    releaseYear = 2008,
                    airDate = "2008-01-20",
                    rating = 8.8f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Walt and Jesse face severe consequences as their meth operation expands and attracts the attention of the DEA and dangerous cartels.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                    episodeCount = 13,
                    releaseYear = 2009,
                    airDate = "2009-03-08",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "Walt is introduced to Gustavo Fring, a major distributor masquerading as a fast-food mogul, leading to lucrative but dangerous employment.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                    episodeCount = 13,
                    releaseYear = 2010,
                    airDate = "2010-03-21",
                    rating = 9.1f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 4,
                    name = "Season 4",
                    overview = "Walt and Gus enter a cold war of psychological warfare as Walt schemes to protect his life and family from Gus's calculated retribution.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                    episodeCount = 13,
                    releaseYear = 2011,
                    airDate = "2011-07-17",
                    rating = 9.4f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 5,
                    name = "Season 5",
                    overview = "Walt embraces the Heisenberg persona fully as the kingpin of his own empire, leading to his inevitable dramatic downfall.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                    episodeCount = 16,
                    releaseYear = 2012,
                    airDate = "2012-07-15",
                    rating = 9.7f
                )
            )

            "tv_the_bear" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "A fine dining young chef comes home to Chicago to run his family Italian beef sandwich shop after a tragic suicide in his family.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/hih5aTwgmHhMuyVSbyaPBnBJftt.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                    episodeCount = 8,
                    releaseYear = 2022,
                    airDate = "2022-06-23",
                    rating = 8.7f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Carmy, Sydney, and Richie work to transform their grimy sandwich joint into a next-level dining spot while shedding past trauma.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/hih5aTwgmHhMuyVSbyaPBnBJftt.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                    episodeCount = 10,
                    releaseYear = 2023,
                    airDate = "2023-06-22",
                    rating = 9.1f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "Carmy pushes himself and his crew harder than ever to achieve culinary perfection and earn a Michelin star.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/hih5aTwgmHhMuyVSbyaPBnBJftt.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                    episodeCount = 10,
                    releaseYear = 2024,
                    airDate = "2024-06-26",
                    rating = 8.5f
                )
            )

            "tv_shogun" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "When a mysterious European ship is found marooned in a fishing village, Lord Toranaga discovers secrets that could tip the scales of power in feudal Japan.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/7O4iVfOMQmdCSxhOg1WnzG1AgYT.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/bwSmgmd90hCWwqOKQYTEraeOZhJ.jpg",
                    episodeCount = 10,
                    releaseYear = 2024,
                    airDate = "2024-02-27",
                    rating = 9.0f
                )
            )

            "tv_severance" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "Mark Scout leads a team of office workers at Lumon Industries whose memories have been surgically divided between their work and personal lives.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/pPHpeI2X1qEd1CS1SeyrdhZ4qnT.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                    episodeCount = 9,
                    releaseYear = 2022,
                    airDate = "2022-02-18",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Mark Scout and his coworkers face the harrowing consequences of their rebellion as Lumon Industries introduces strict new protocols on the severed floor.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/pPHpeI2X1qEd1CS1SeyrdhZ4qnT.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                    episodeCount = 10,
                    releaseYear = 2025,
                    airDate = "2025-01-17",
                    rating = 9.1f
                )
            )

            "tv_squid_game" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "Hundreds of cash-strapped players accept a strange invitation to compete in children's games for a tempting prize with deadly high stakes.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1QdXdRYfktUSONkl1oD5gc6Be0s.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/5aE1kxWg6RhgQxJTXTxifv4uq7P.jpg",
                    episodeCount = 9,
                    releaseYear = 2021,
                    airDate = "2021-09-17",
                    rating = 8.8f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Three years after winning the Squid Game, Player 456 gave up going to the United States and is back with a new resolution in his mind.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1QdXdRYfktUSONkl1oD5gc6Be0s.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/5aE1kxWg6RhgQxJTXTxifv4uq7P.jpg",
                    episodeCount = 6,
                    releaseYear = 2024,
                    airDate = "2024-12-26",
                    rating = 8.6f
                )
            )

            "tv_dark" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "A missing child sets four families on a frantic hunt for answers as they unearth a mind-bending mystery that spans three generations in Winden.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/eRtnI7qTJIiPUmlY3J8TTM3jlT7.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/fmFsm4dq02k2Xxs7M887unPAiXd.jpg",
                    episodeCount = 10,
                    releaseYear = 2017,
                    airDate = "2017-12-01",
                    rating = 8.8f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Jonas finds himself trapped in the future and frantically tries to return to 2020. Meanwhile, his friends in Winden investigate the mysterious cave.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/eRtnI7qTJIiPUmlY3J8TTM3jlT7.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/fmFsm4dq02k2Xxs7M887unPAiXd.jpg",
                    episodeCount = 8,
                    releaseYear = 2019,
                    airDate = "2019-06-21",
                    rating = 9.0f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "The final cycle begins as Jonas arrives in a parallel world where Martha exists, racing to untangle the knotted knot once and for all.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/eRtnI7qTJIiPUmlY3J8TTM3jlT7.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/fmFsm4dq02k2Xxs7M887unPAiXd.jpg",
                    episodeCount = 8,
                    releaseYear = 2020,
                    airDate = "2020-06-27",
                    rating = 8.9f
                )
            )

            "tv_peaky_blinders" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Series 1",
                    overview = "Thomas Shelby leads the Peaky Blinders gang in 1919 Birmingham, England, aiming to build an empire from illegal betting.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/vUUqzWa2LnHIVqkaKVlVGkVcZIW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
                    episodeCount = 6,
                    releaseYear = 2013,
                    airDate = "2013-09-12",
                    rating = 8.7f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Series 2",
                    overview = "The Shelby family expands their business into London, facing vicious rival gangs and Chief Inspector Campbell.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/vUUqzWa2LnHIVqkaKVlVGkVcZIW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
                    episodeCount = 6,
                    releaseYear = 2014,
                    airDate = "2014-10-02",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Series 3",
                    overview = "Tommy is pulled into international intrigue and a dangerous web of White Russians and secret societies.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/vUUqzWa2LnHIVqkaKVlVGkVcZIW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
                    episodeCount = 6,
                    releaseYear = 2016,
                    airDate = "2016-05-05",
                    rating = 8.8f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 4,
                    name = "Series 4",
                    overview = "Luca Changretta arrives from New York seeking a blood vendetta against the Shelby family.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/vUUqzWa2LnHIVqkaKVlVGkVcZIW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
                    episodeCount = 6,
                    releaseYear = 2017,
                    airDate = "2017-11-15",
                    rating = 9.2f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 5,
                    name = "Series 5",
                    overview = "The Wall Street Crash of 1929 brings new dangers and opportunities as Tommy enters British politics.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/vUUqzWa2LnHIVqkaKVlVGkVcZIW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
                    episodeCount = 6,
                    releaseYear = 2019,
                    airDate = "2019-08-25",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 6,
                    name = "Series 6",
                    overview = "Tommy faces down old demons and political enemies in a treacherous final chapter before World War II.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/vUUqzWa2LnHIVqkaKVlVGkVcZIW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/dzq83RHwQcnP6WGJ6YkenIqeaa5.jpg",
                    episodeCount = 6,
                    releaseYear = 2022,
                    airDate = "2022-02-27",
                    rating = 9.0f
                )
            )

            "tv_mirzapur" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "Akhandanand Tripathi made it big from carpets and guns. His prodigal son Munna is hungry to inherit the reign of Mirzapur.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/p0qM8hhlMF5DuxHBzl2EZR6TehX.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/3dV7pWAdwIPKR2lMIACMfObXdgK.jpg",
                    episodeCount = 9,
                    releaseYear = 2018,
                    airDate = "2018-11-16",
                    rating = 8.5f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Guddu Pandit and Golu Gupta emerge from grief with a burning desire to dismantle the Tripathi empire.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/p0qM8hhlMF5DuxHBzl2EZR6TehX.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/3dV7pWAdwIPKR2lMIACMfObXdgK.jpg",
                    episodeCount = 10,
                    releaseYear = 2020,
                    airDate = "2020-10-23",
                    rating = 8.6f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "Guddu claims the throne of Purvanchal, but rivals from Jaunpur and Siwan circle like vultures.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/p0qM8hhlMF5DuxHBzl2EZR6TehX.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/3dV7pWAdwIPKR2lMIACMfObXdgK.jpg",
                    episodeCount = 10,
                    releaseYear = 2024,
                    airDate = "2024-07-05",
                    rating = 8.3f
                )
            )

            "tv_panchayat" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "Engineering graduate Abhishek Tripathi lands the low-paying job of Panchayat Secretary in remote rural Phulera, Uttar Pradesh.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/8ivTX5q2otqXvH815BcOhxVkTPW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/iZ8EtGAqKWZdRJPzWfFseNfVxjh.jpg",
                    episodeCount = 8,
                    releaseYear = 2020,
                    airDate = "2020-04-03",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "Abhishek embraces village politics, personal friendships, and unexpected emotional challenges while studying for CAT.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/8ivTX5q2otqXvH815BcOhxVkTPW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/iZ8EtGAqKWZdRJPzWfFseNfVxjh.jpg",
                    episodeCount = 8,
                    releaseYear = 2022,
                    airDate = "2022-05-18",
                    rating = 9.2f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "With local elections looming, the political rift between Pradhan ji and Banrakas intensifies dramatically.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/8ivTX5q2otqXvH815BcOhxVkTPW.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/iZ8EtGAqKWZdRJPzWfFseNfVxjh.jpg",
                    episodeCount = 8,
                    releaseYear = 2024,
                    airDate = "2024-05-28",
                    rating = 9.0f
                )
            )

            "tv_money_heist" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Part 1",
                    overview = "The Professor recruits eight skilled thieves to execute an audacious heist: printing 2.4 billion euros at the Royal Mint of Spain.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/xGexTKCJDkl12dTW4YCBDXWb1AD.jpg",
                    episodeCount = 9,
                    releaseYear = 2017,
                    airDate = "2017-05-02",
                    rating = 8.8f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Part 2",
                    overview = "As the police close in on the Professor's identity, tension inside the Royal Mint reaches a fever pitch.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/xGexTKCJDkl12dTW4YCBDXWb1AD.jpg",
                    episodeCount = 6,
                    releaseYear = 2017,
                    airDate = "2017-10-16",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Part 3",
                    overview = "The crew reunites to rescue Rio and execute an even more daring target: the Bank of Spain's underground gold vault.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/xGexTKCJDkl12dTW4YCBDXWb1AD.jpg",
                    episodeCount = 8,
                    releaseYear = 2019,
                    airDate = "2019-07-19",
                    rating = 8.7f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 4,
                    name = "Part 4",
                    overview = "Lives are on the line as internal conflict and an external adversary threaten to destroy the Professor's plan.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/xGexTKCJDkl12dTW4YCBDXWb1AD.jpg",
                    episodeCount = 8,
                    releaseYear = 2020,
                    airDate = "2020-04-03",
                    rating = 8.5f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 5,
                    name = "Part 5",
                    overview = "Sierra captures the Professor as the army storms the Bank of Spain in a war that pushes the gang to the brink.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/xGexTKCJDkl12dTW4YCBDXWb1AD.jpg",
                    episodeCount = 10,
                    releaseYear = 2021,
                    airDate = "2021-09-03",
                    rating = 8.6f
                )
            )

            "tv_the_crown" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "A young Princess Elizabeth marries Philip and ascends the British throne amid postwar political rivalries.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1M876KPjulVwppEpldhdc8V4o68.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/8VXhcrl5z2I1zEU9X3pkkNrZlD.jpg",
                    episodeCount = 10,
                    releaseYear = 2016,
                    airDate = "2016-11-04",
                    rating = 8.7f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 2,
                    name = "Season 2",
                    overview = "As the Suez Crisis threatens Britain's global standing, Elizabeth navigates personal and political tensions.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1M876KPjulVwppEpldhdc8V4o68.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/8VXhcrl5z2I1zEU9X3pkkNrZlD.jpg",
                    episodeCount = 10,
                    releaseYear = 2017,
                    airDate = "2017-12-08",
                    rating = 8.6f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 3,
                    name = "Season 3",
                    overview = "The Queen guides the realm through the tumultuous 1960s, Harold Wilson's government, and the moon landing.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1M876KPjulVwppEpldhdc8V4o68.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/8VXhcrl5z2I1zEU9X3pkkNrZlD.jpg",
                    episodeCount = 10,
                    releaseYear = 2019,
                    airDate = "2019-11-17",
                    rating = 8.4f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 4,
                    name = "Season 4",
                    overview = "The arrival of Lady Diana Spencer and Prime Minister Margaret Thatcher reshapes the royal dynamic in the 1980s.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1M876KPjulVwppEpldhdc8V4o68.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/8VXhcrl5z2I1zEU9X3pkkNrZlD.jpg",
                    episodeCount = 10,
                    releaseYear = 2020,
                    airDate = "2020-11-15",
                    rating = 8.9f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 5,
                    name = "Season 5",
                    overview = "The royal family enters the turbulent 1990s as public scrutiny and marital crises challenge the monarchy.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1M876KPjulVwppEpldhdc8V4o68.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/8VXhcrl5z2I1zEU9X3pkkNrZlD.jpg",
                    episodeCount = 10,
                    releaseYear = 2022,
                    airDate = "2022-11-09",
                    rating = 8.3f
                ),
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 6,
                    name = "Season 6",
                    overview = "The final chapter depicts the tragic loss of Princess Diana, royal reflection, and the Golden Jubilee.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/1M876KPjulVwppEpldhdc8V4o68.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/8VXhcrl5z2I1zEU9X3pkkNrZlD.jpg",
                    episodeCount = 10,
                    releaseYear = 2023,
                    airDate = "2023-11-16",
                    rating = 8.5f
                )
            )

            "tv_queen_of_tears" -> listOf(
                SeasonItem(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "The queen of department stores Hong Hae-in and small-town legal director Baek Hyun-woo weather a marriage crisis until love begins to bloom anew.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/7ZXLZ3KYL3IVvsSHBZaHjcNQzNU.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/wcP3FsRLog4GNEs9PFrDKKQdcof.jpg",
                    episodeCount = 16,
                    releaseYear = 2024,
                    airDate = "2024-03-09",
                    rating = 8.8f
                )
            )

            else -> generateGenericSeasons(media)
        }
    }

    fun getCuratedEpisodes(media: MediaEntity, seasonNumber: Int): List<EpisodeItem> {
        val seriesId = media.id
        val seriesTitle = media.title
        val backdrop = media.backdropUrl

        val episodes = when (seriesId) {
            "tv_stranger_things" -> when (seasonNumber) {
                1 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s1_e1",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Chapter One: The Vanishing of Will Byers",
                        overview = "On his way home from a friend's house, young Will sees something terrifying. Nearby, a sinister secret lurks in the depths of a government lab.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 49,
                        rating = 8.5f,
                        director = "The Duffer Brothers",
                        writer = "The Duffer Brothers"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e2",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Chapter Two: The Weirdo on Maple Street",
                        overview = "Lucas, Mike and Dustin try to talk to the girl they found in the woods. Hopper questions an anxious Joyce about an unsettling phone call.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 56,
                        rating = 8.3f,
                        director = "The Duffer Brothers",
                        writer = "The Duffer Brothers"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e3",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 3,
                        title = "Chapter Three: Holly, Jolly",
                        overview = "An increasingly frantic Joyce seeks answers through Christmas lights. Eleven shows the boys where Will is hiding on the board.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 52,
                        rating = 8.7f,
                        director = "Shawn Levy",
                        writer = "Jessica Mecklenburg"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e4",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 4,
                        title = "Chapter Four: The Body",
                        overview = "Refusing to believe Will is dead, Joyce tries to connect with her son. The boys give Eleven a makeover before sneaking into school.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 51,
                        rating = 8.9f,
                        director = "Shawn Levy",
                        writer = "Justin Doble"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e5",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 5,
                        title = "Chapter Five: The Flea and the Acrobat",
                        overview = "Hopper breaks into the lab while Nancy and Jonathan confront the force that took Will. The boys ask Mr. Clarke about traveling across dimensions.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 53,
                        rating = 8.7f,
                        director = "The Duffer Brothers",
                        writer = "Alison Tatlock"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e6",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 6,
                        title = "Chapter Six: The Monster",
                        overview = "A frantic Jonathan looks for Nancy in the woods. Steve suspects Nancy is cheating. Hopper and Joyce uncover the truth about Eleven's past.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 47,
                        rating = 8.9f,
                        director = "The Duffer Brothers",
                        writer = "Jessie Nickson-Lopez"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e7",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 7,
                        title = "Chapter Seven: The Bathtub",
                        overview = "Eleven uses her powers to reach Will and Barb while government agents close in on the Byers home. The kids build a makeshift sensory deprivation tank.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 42,
                        rating = 9.1f,
                        director = "The Duffer Brothers",
                        writer = "Justin Doble"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e8",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 8,
                        title = "Chapter Eight: The Upside Down",
                        overview = "Dr. Brenner holds Hopper and Joyce for interrogation while the boys wait with Eleven in the gym. A deadly battle erupts against the Demogorgon.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2016-07-15",
                        runtimeMinutes = 55,
                        rating = 9.4f,
                        director = "The Duffer Brothers",
                        writer = "The Duffer Brothers"
                    )
                )
                4 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s4_e1",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 4,
                        episodeNumber = 1,
                        title = "Chapter One: The Hellfire Club",
                        overview = "El struggles to fit in at school in California. Back in Hawkins, high school basketball and D&D divide the gang. A terrifying new threat appears.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2022-05-27",
                        runtimeMinutes = 78,
                        rating = 8.8f,
                        director = "The Duffer Brothers",
                        writer = "The Duffer Brothers"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s4_e4",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 4,
                        episodeNumber = 4,
                        title = "Chapter Four: Dear Billy",
                        overview = "Max is in grave danger and running out of time. A patient at Pennhurst asylum has answers. Hopper works hard in Russia.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2022-05-27",
                        runtimeMinutes = 79,
                        rating = 9.6f,
                        director = "Shawn Levy",
                        writer = "Paul Dichter"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s4_e7",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 4,
                        episodeNumber = 7,
                        title = "Chapter Seven: The Massacre at Hawkins Lab",
                        overview = "As Hopper braces to battle a monster, Dustin dissects Vecna's motives. An El memory reveals the truth about Number One.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2022-05-27",
                        runtimeMinutes = 100,
                        rating = 9.7f,
                        director = "The Duffer Brothers",
                        writer = "The Duffer Brothers"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s4_e9",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 4,
                        episodeNumber = 9,
                        title = "Chapter Nine: The Piggyback",
                        overview = "With selfless hearts and a clash of metal, heroes fight from every corner of the battlefield to save Hawkins — and the world.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                        airDate = "2022-07-01",
                        runtimeMinutes = 150,
                        rating = 9.3f,
                        director = "The Duffer Brothers",
                        writer = "The Duffer Brothers"
                    )
                )
                else -> generateGenericEpisodesForSeason(media, seasonNumber, 8)
            }

            "tv_breaking_bad" -> when (seasonNumber) {
                1 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s1_e1",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Pilot",
                        overview = "When an unassuming high school chemistry teacher is diagnosed with terminal lung cancer, he decides to secure his family's future with meth.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-01-20",
                        runtimeMinutes = 58,
                        rating = 9.1f,
                        director = "Vince Gilligan",
                        writer = "Vince Gilligan"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e2",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Cat's in the Bag...",
                        overview = "Walt and Jesse attempt to dispose of two bodies in the RV, but things get messy when Jesse ignores Walt's specific instructions regarding acid.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-01-27",
                        runtimeMinutes = 48,
                        rating = 8.7f,
                        director = "Adam Bernstein",
                        writer = "Vince Gilligan"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e3",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 3,
                        title = "...And the Bag's in the River",
                        overview = "Walt bonds with Krazy-8 in Jesse's basement while struggling with the moral dilemma of whether to kill him or set him free.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-02-10",
                        runtimeMinutes = 48,
                        rating = 8.9f,
                        director = "Adam Bernstein",
                        writer = "Vince Gilligan"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e4",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 4,
                        title = "Cancer Man",
                        overview = "Walt tells his family about his cancer during an emotional intervention. Jesse struggles to stay clean while visiting his wealthy parents.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-02-17",
                        runtimeMinutes = 48,
                        rating = 8.3f,
                        director = "Jim McKay",
                        writer = "Vince Gilligan"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e5",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 5,
                        title = "Gray Matter",
                        overview = "Walt attends a lavish birthday party for his former business partner, Elliot Schwartz, and turns down a charitable job offer.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-02-24",
                        runtimeMinutes = 48,
                        rating = 8.4f,
                        director = "Tricia Brock",
                        writer = "Patty Lin"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e6",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 6,
                        title = "Crazy Handful of Nothin'",
                        overview = "Walt shaves his head as chemo starts and adopts the alias 'Heisenberg' before introducing Tuco Salamanca to fulminated mercury.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-03-02",
                        runtimeMinutes = 48,
                        rating = 9.3f,
                        director = "Bronwen Hughes",
                        writer = "George Mastras"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e7",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 7,
                        title = "A No-Rough-Stuff-Type Deal",
                        overview = "Walt and Jesse face logistical hurdles in securing methylamine for their burgeoning supply chain, culminating in a junkyard showdown.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2008-03-09",
                        runtimeMinutes = 48,
                        rating = 8.9f,
                        director = "Tim Hunter",
                        writer = "Peter Gould"
                    )
                )
                5 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s5_e14",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 5,
                        episodeNumber = 14,
                        title = "Ozymandias",
                        overview = "Everyone copes with the cataclysmic fallout of the desert shootout in radically different ways. Walter's family crumbles.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2013-09-15",
                        runtimeMinutes = 48,
                        rating = 10.0f,
                        director = "Rian Johnson",
                        writer = "Moira Walley-Beckett"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s5_e15",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 5,
                        episodeNumber = 15,
                        title = "Granite State",
                        overview = "Walt lives in isolation in a remote cabin in New Hampshire as news of his empire dominates national headlines.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2013-09-22",
                        runtimeMinutes = 55,
                        rating = 9.7f,
                        director = "Peter Gould",
                        writer = "Peter Gould"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s5_e16",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 5,
                        episodeNumber = 16,
                        title = "Felina",
                        overview = "Walter White returns to Albuquerque to settle all scores, protect what remains of his money, and free Jesse Pinkman.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
                        airDate = "2013-09-29",
                        runtimeMinutes = 55,
                        rating = 9.9f,
                        director = "Vince Gilligan",
                        writer = "Vince Gilligan"
                    )
                )
                else -> generateGenericEpisodesForSeason(media, seasonNumber, 13)
            }

            "tv_the_bear" -> when (seasonNumber) {
                1 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s1_e1",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "System",
                        overview = "Carmy attempts to bring order to the chaotic kitchen of The Original Beef of Chicagoland, clashing with stubborn longtime employees.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                        airDate = "2022-06-23",
                        runtimeMinutes = 29,
                        rating = 8.5f,
                        director = "Christopher Storer",
                        writer = "Christopher Storer"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e2",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Hands",
                        overview = "A surprise health inspection reveals deep-seated cracks in the restaurant's foundation. Carmy hires ambitious culinary grad Sydney.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                        airDate = "2022-06-23",
                        runtimeMinutes = 30,
                        rating = 8.4f,
                        director = "Christopher Storer",
                        writer = "Christopher Storer"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e7",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 7,
                        title = "Review",
                        overview = "Sydney's new to-go ordering system goes live during a hectic morning rush, triggering a single-take descent into complete kitchen meltdown.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                        airDate = "2022-06-23",
                        runtimeMinutes = 20,
                        rating = 9.6f,
                        director = "Christopher Storer",
                        writer = "Joanna Calo"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e8",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 8,
                        title = "Braciole",
                        overview = "Carmy attends Al-Anon and opens up about his grief for Mikey. A shocking culinary discovery in tomato sauce cans changes everything.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                        airDate = "2022-06-23",
                        runtimeMinutes = 48,
                        rating = 9.3f,
                        director = "Christopher Storer",
                        writer = "Christopher Storer"
                    )
                )
                2 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s2_e6",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 2,
                        episodeNumber = 6,
                        title = "Fishes",
                        overview = "In a chaotic flashback to Christmas past, Carmy and the Berzatto family navigate grief, tension, and family dysfunction over a seven fishes feast.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                        airDate = "2023-06-22",
                        runtimeMinutes = 66,
                        rating = 9.7f,
                        director = "Christopher Storer",
                        writer = "Joanna Calo & Christopher Storer"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s2_e7",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 2,
                        episodeNumber = 7,
                        title = "Forks",
                        overview = "Richie spends a life-altering week staging at the world's most elite three-Michelin-starred fine dining establishment.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/aJtG4txtmiRHwAAqENQHZvBs6kY.jpg",
                        airDate = "2023-06-22",
                        runtimeMinutes = 36,
                        rating = 9.7f,
                        director = "Christopher Storer",
                        writer = "Alex O'Keefe"
                    )
                )
                else -> generateGenericEpisodesForSeason(media, seasonNumber, 10)
            }

            "tv_shogun" -> listOf(
                EpisodeItem(
                    id = "${seriesId}_s1_e1",
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    title = "Chapter One: Anjin",
                    overview = "Destiny brings a mysterious English ship to Japan. Lord Toranaga schemes against five rival regents holding him under siege at Osaka Castle.",
                    stillUrl = "https://image.tmdb.org/t/p/w780/bwSmgmd90hCWwqOKQYTEraeOZhJ.jpg",
                    airDate = "2024-02-27",
                    runtimeMinutes = 70,
                    rating = 8.8f,
                    director = "Jonathan van Tulleken",
                    writer = "Rachel Kondo & Justin Marks"
                ),
                EpisodeItem(
                    id = "${seriesId}_s1_e2",
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    episodeNumber = 2,
                    title = "Chapter Two: Servants of Two Masters",
                    overview = "Blackthorne's arrival in Osaka stirs a hornet's nest of rivalries. Mariko must balance her Christian faith and feudal loyalty.",
                    stillUrl = "https://image.tmdb.org/t/p/w780/bwSmgmd90hCWwqOKQYTEraeOZhJ.jpg",
                    airDate = "2024-02-27",
                    runtimeMinutes = 58,
                    rating = 8.9f,
                    director = "Jonathan van Tulleken",
                    writer = "Rachel Kondo & Justin Marks"
                ),
                EpisodeItem(
                    id = "${seriesId}_s1_e9",
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    episodeNumber = 9,
                    title = "Chapter Nine: Crimson Sky",
                    overview = "Mariko arrives in Osaka for the fight of her life. Blackthorne and Yabushige struggle to salvage their dwindling options.",
                    stillUrl = "https://image.tmdb.org/t/p/w780/bwSmgmd90hCWwqOKQYTEraeOZhJ.jpg",
                    airDate = "2024-04-16",
                    runtimeMinutes = 59,
                    rating = 9.6f,
                    director = "Frederick E.O. Toye",
                    writer = "Rachel Kondo & Caillin Puente"
                ),
                EpisodeItem(
                    id = "${seriesId}_s1_e10",
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seasonNumber = 1,
                    episodeNumber = 10,
                    title = "Chapter Ten: A Dream of a Dream",
                    overview = "In the wake of a tragic death, Blackthorne considers his purpose and Toranaga reveals his ultimate master plan.",
                    stillUrl = "https://image.tmdb.org/t/p/w780/bwSmgmd90hCWwqOKQYTEraeOZhJ.jpg",
                    airDate = "2024-04-23",
                    runtimeMinutes = 61,
                    rating = 9.2f,
                    director = "Frederick E.O. Toye",
                    writer = "Maegan Houang & Emily Yoshida"
                )
            )

            "tv_severance" -> when (seasonNumber) {
                1 -> listOf(
                    EpisodeItem(
                        id = "${seriesId}_s1_e1",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Good News About Hell",
                        overview = "Mark Scout leads a team at Lumon Industries whose employees have undergone a severance procedure that divides memories between home and work.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-02-18",
                        runtimeMinutes = 57,
                        rating = 8.6f,
                        director = "Ben Stiller",
                        writer = "Dan Erickson"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e2",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Half Loop",
                        overview = "The Macrodata Refinement team trains Helly on macrodata refinement while Mark meets with a mysterious former colleague on the outside.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-02-18",
                        runtimeMinutes = 53,
                        rating = 8.5f,
                        director = "Ben Stiller",
                        writer = "Dan Erickson"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e3",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 3,
                        title = "In Perpetuity",
                        overview = "Mark takes the team on a field trip to the Perpetuity Wing. Cobel escalates her surveillance of Mark's outie life.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-02-25",
                        runtimeMinutes = 54,
                        rating = 8.7f,
                        director = "Ben Stiller",
                        writer = "Andrew Colville"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e4",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 4,
                        title = "The You You Are",
                        overview = "Helly undergoes a disciplinary session in the Break Room. Mark discovers an illicit self-help book left behind by Cobel.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-03-04",
                        runtimeMinutes = 53,
                        rating = 8.8f,
                        director = "Aoife McArdle",
                        writer = "Kari Drake"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e5",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 5,
                        title = "The Grim Barbarity of Optics and Design",
                        overview = "Irving forms an unexpected connection with Burt in Optics and Design. Dylan and Mark question Lumon's official mythology.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-03-11",
                        runtimeMinutes = 54,
                        rating = 8.6f,
                        director = "Aoife McArdle",
                        writer = "Anna Ouyang Moench"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e6",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 6,
                        title = "Hide and Seek",
                        overview = "Cobel faces scrutiny from the Lumon board. The team forms an alliance with Optics and Design across department lines.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-03-18",
                        runtimeMinutes = 50,
                        rating = 8.9f,
                        director = "Aoife McArdle",
                        writer = "Amanda Overton"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e7",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 7,
                        title = "Defiant Jazz",
                        overview = "Milchick throws a Music Dance Experience for the department, which spirals out of control when Dylan uncovers hidden truths.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-03-25",
                        runtimeMinutes = 53,
                        rating = 9.5f,
                        director = "Ben Stiller",
                        writer = "Helen Leigh"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e8",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 8,
                        title = "What's for Dinner?",
                        overview = "The team prepares for the critical Overtime Contingency protocol. Mark attends a dinner party with his sister Devon.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-04-01",
                        runtimeMinutes = 55,
                        rating = 9.3f,
                        director = "Ben Stiller",
                        writer = "Chris Black"
                    ),
                    EpisodeItem(
                        id = "${seriesId}_s1_e9",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 1,
                        episodeNumber = 9,
                        title = "The We We Are",
                        overview = "The Macrodata Refinement innies activate the Overtime Contingency protocol in the outside world to uncover the truth about their identities.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2022-04-08",
                        runtimeMinutes = 43,
                        rating = 9.7f,
                        director = "Ben Stiller",
                        writer = "Dan Erickson"
                    )
                )
                2 -> (1..10).map { epNum ->
                    val epTitles = listOf(
                        "Goodbye Lumon",
                        "The Innie Awakening",
                        "Reintegration",
                        "The Testing Floor",
                        "Department of the Unsevered",
                        "Board Directive",
                        "Kier's Protocol",
                        "The Overtime Contingency",
                        "The Subterfuge",
                        "The Cold Harbor"
                    )
                    EpisodeItem(
                        id = "${seriesId}_s2_e$epNum",
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        seasonNumber = 2,
                        episodeNumber = epNum,
                        title = epTitles.getOrElse(epNum - 1) { "Episode $epNum" },
                        overview = "Mark Scout and his coworkers navigate the escalating aftermath of their rebellion on Lumon's severed floor as deep secrets surface.",
                        stillUrl = "https://image.tmdb.org/t/p/w780/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
                        airDate = "2025-0${((epNum - 1) % 3) + 1}-17",
                        runtimeMinutes = 52 + (epNum % 8),
                        rating = 9.0f + ((epNum % 5) * 0.1f),
                        director = "Ben Stiller",
                        writer = "Dan Erickson"
                    )
                }
                else -> generateGenericEpisodesForSeason(media, seasonNumber, 9)
            }

            else -> generateGenericEpisodesForSeason(
                media = media,
                seasonNumber = seasonNumber,
                count = if (media.totalEpisodes > 0 && media.totalSeasons > 0) {
                    (media.totalEpisodes / media.totalSeasons).coerceIn(4, 16)
                } else 8
            )
        }

        return episodes.ifEmpty {
            generateGenericEpisodesForSeason(media, seasonNumber, 8)
        }
    }

    private fun generateGenericSeasons(media: MediaEntity): List<SeasonItem> {
        val totalSeasons = maxOf(1, media.totalSeasons)
        val episodesPerSeason = if (media.totalEpisodes > 0) {
            maxOf(1, (media.totalEpisodes + totalSeasons - 1) / totalSeasons)
        } else 8

        val startYear = media.releaseYear

        return (1..totalSeasons).map { sNum ->
            val seasonYear = startYear + (sNum - 1)
            SeasonItem(
                seriesId = media.id,
                seriesTitle = media.title,
                seasonNumber = sNum,
                name = "Season $sNum",
                overview = if (sNum == 1) {
                    media.overview.ifBlank { "The premiere season introducing the world, secrets, and characters of ${media.title}." }
                } else {
                    "Season $sNum continues the escalating story of ${media.title} with higher stakes, deeper conflicts, and critical turning points."
                },
                posterUrl = media.posterUrl,
                backdropUrl = media.backdropUrl,
                episodeCount = episodesPerSeason,
                releaseYear = seasonYear,
                airDate = "$seasonYear-0${(sNum % 9) + 1}-15",
                rating = (media.communityRating - (sNum * 0.1f)).coerceIn(7.0f, 9.8f)
            )
        }
    }

    private fun generateGenericEpisodesForSeason(media: MediaEntity, seasonNumber: Int, count: Int): List<EpisodeItem> {
        val titles = listOf(
            "The Beginning of the End",
            "Shadows and Whispers",
            "Point of No Return",
            "A House Divided",
            "The Reckoning",
            "Ghosts of the Past",
            "Under the Surface",
            "Crossroads",
            "The Turning Point",
            "Into the Abyss",
            "The Gathering Storm",
            "Truth and Consequence",
            "Final Gambit",
            "Echoes of Deception",
            "Ascension",
            "The Endgame"
        )

        val year = media.releaseYear + (seasonNumber - 1)

        return (1..count).map { epNum ->
            val title = if (epNum <= titles.size) titles[epNum - 1] else "Episode $epNum"
            val runtime = 42 + ((epNum * 7) % 25)
            val month = ((epNum % 12) + 1).toString().padStart(2, '0')
            val day = ((epNum * 3) % 27 + 1).toString().padStart(2, '0')

            EpisodeItem(
                id = "${media.id}_s${seasonNumber}_e${epNum}",
                seriesId = media.id,
                seriesTitle = media.title,
                seasonNumber = seasonNumber,
                episodeNumber = epNum,
                title = title,
                overview = "In this chapter of ${media.title}, unexpected developments test alliances and force dangerous choices as secrets are uncovered.",
                stillUrl = media.backdropUrl.ifBlank { media.posterUrl },
                airDate = "$year-$month-$day",
                runtimeMinutes = runtime,
                rating = (media.communityRating + ((epNum % 5) * 0.15f) - 0.3f).coerceIn(7.2f, 9.6f),
                director = media.directorOrCreator.ifBlank { "Executive Production" },
                writer = media.directorOrCreator.ifBlank { "Screenplay Team" }
            )
        }
    }
}
