= Animals =

== Birds ==
bird-megataiga (f): "mega*taiga"|temp >= 0.2-0.5|day:1|night:0.25;
bird-taiga (f): "taiga",#forest|temp:>0.1-0.5|day:1|night:0.25;
bird-roofed (f): "roofed*forest"|temp:>0.5-1|day:1|night:0.25;
bird-swamp (f): "swamp"|temp:>0.5-2|day:1|night:0.25;
bird-jungle (f): "jungle"|temp:>0.5-2|day:1|night:0;
bird-jungle-night (f): "jungle"|temp:>0.5-2|day:0|night:1;
bird (f): #forest|temp:>0.5-1|day:1|night:0.25;
bird-cold (f): "plains"|0.3|temp:>0.5-1|day:1|night:0;#forest|temp:>=0.1-0.5|day:1|night:0.25;
crow (f): #forest|temp: <=0.1|day:1|night:0.5;
bird-warm (f): #forest|temp:>1-3|day:1|night:0.5;"savanna"|0.3|temp:>1-2|day:1|night:0;

== Cicadas ==
cicadas: "plains","savanna"|temp:>0.5-2|day:0.1|night:0.5;
cicadas-desert: #desert,"mesa"|temp:>0.5;

== Crickets ==
cricket-swamp: "swamp"|temp:>0.5-2|day:1|night:0.5;
cricket-jungle: "jungle"|temp:>0.5-2|day:1|night:0;
cricket-jungle-night: "jungle"|temp:>0.5-2|day:0|night:1;
cricket-forest-night: #forest|temp:>0.1-1.5|day:0|night:1;
cricket: #forest,"plains"|temp:>0.1-1.5|day:0.3|night:0.15;
cricket-warm-night: "swamp"|temp:>0.5-2|day:0|night:0.5;#forest,"plains"|temp:>0.5-1.5|day:0|night:1;
cricket-night: #forest,"plains"|temp:>0.1-0.5|day:0|night:1;

== Frogs ==
frog1,frog2 p(100,750,100): "swamp"|temp:>0.5-2|day:0.25|night:0.5;

== Owl ==
owl1,owl2,owl3,owl4,owl5,owl6,owl7,owl8,owl9 i(10,200): #forest|temp:>0.1-1.5|day:0|night:1;

== Seagul ==
seagull1,seagull2,seagull3,seagull4,seagull5 i(100,750): #beach|temp:>0.1-1.5;
seagull-long p(100,750,100): #beach|temp:>0.1-1.5;

== Wolf ==
wolf1,wolf2,wolf3,wolf4,wolf5,wolf6,wolf7,wolf8,wolf9,wolf10 i(10,200): "plains","savanna"|temp:>1-2|day:0|night:1;

= Suspense =
nether (f): "hell";
end (f): "end";
cave1,cave2 (f): #dark;

= Water =
beach (f): #beach:temp:>0;
ocean (f): "ocean":temp:>0;
underwater (f): #underwater|mute:0.9;

= Weather =
rain: #rain|mute:0.5;
storm-close: #thunder|surface|mute:0.5;
storm-away: #thunder|underground|mute:0.5;

= Wind =
heavy-wind: "extreme hills";
howling-wind: unused
light-wind: #desert,#snow;
mesa: "mesa";
steady-wind: "ocean"|0.4;#intheair|0.2;

#intheair: a few blocks above the surface
#thunder: thunder and rain
#rain: if it's raining
#underwater: if the player is underwater
#dark: below surface or dark areas
#desert: hot biomes with are not covered by grass
#snow: anything covered by snow
#beach: biomes which are basically a beach.
#forest: biomes with grass as surface and enabled tree generation or the name contains "forest"
f: plays continously (only one can be played for each biome)
p(minT,maxT,length): partially played (from time to time +fade in and out) min and max time between the sound being played. length determines how long the sound will be played, fade in and out are 10% each.
i(minT,maxT): individual files, no stream. min and max time between the sound being played.