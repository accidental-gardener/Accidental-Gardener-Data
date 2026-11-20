"""Generates plants.json for The Accidental Gardener.

Each plant has its planner data plus a growing guide. Common steps come from
templates below; plant-specific advice is written per plant.
"""
import json, sys

VERSION = 8
UPDATED = "2026-11-20"
PLANTS = []


# Too big or deep rooted for a pot
NOT_IN_POTS = {"pumpkin", "squash", "marrow", "sweetcorn", "maincrop", "parsnip", "sprouts", "psb", "cauliflower",
               "cabbage", "savoy", "redcabbage", "romanesco", "celeriac", "asparagus", "artichoke", "jerusalem",
               "rhubarb", "pear", "plum", "cherry", "raspberry", "blackberry", "tayberry", "broadbean", "runnerbean",
               "borlotti", "swede", "leek", "salsify", "lovage", "wildgarlic", "grape", "sunflower", "phacelia",
               "foxglove", "aster", "springcabbage", "cavolonero"}

# Good for bees, butterflies and hoverflies: (seasons in flower, months in flower)
POLLINATORS = {
    "camassia": (["SPRING", "SUMMER"], "May to Jun"),
    "borage": (["SUMMER"], "Jun to Sep"), "phacelia": (["SUMMER"], "Jun to Aug"), "lavender": (["SUMMER"], "Jun to Aug"),
    "marigold": (["SUMMER", "AUTUMN"], "Jun to Oct"), "calendula": (["SUMMER", "AUTUMN"], "Jun to Oct"),
    "cosmos": (["SUMMER", "AUTUMN"], "Jul to Oct"), "nasturtium": (["SUMMER", "AUTUMN"], "Jun to Oct"),
    "sunflower": (["SUMMER"], "Jul to Sep"), "cornflower": (["SUMMER"], "Jun to Aug"), "poppy": (["SUMMER"], "Jun to Aug"),
    "echinacea": (["SUMMER", "AUTUMN"], "Jul to Sep"), "lupin": (["SUMMER"], "May to Jul"),
    "foxglove": (["SUMMER"], "Jun to Jul"), "wallflower": (["SPRING"], "Mar to May"), "nigella": (["SUMMER"], "Jun to Aug"),
    "zinnia": (["SUMMER"], "Jul to Sep"), "dahlia": (["SUMMER", "AUTUMN"], "Jul to Oct, single-flowered types"),
    "chives": (["SUMMER"], "May to Jul"), "garlicchives": (["SUMMER"], "Aug to Sep"), "thyme": (["SUMMER"], "May to Aug"),
    "oregano": (["SUMMER"], "Jul to Sep"), "marjoram": (["SUMMER"], "Jul to Sep"), "rosemary": (["SPRING"], "Mar to May"),
    "sage": (["SUMMER"], "Jun to Jul"), "mint": (["SUMMER"], "Jul to Sep"), "dill": (["SUMMER"], "Jul to Aug"),
    "coriander": (["SUMMER"], "Jun to Aug"), "lemonbalm": (["SUMMER"], "Jun to Aug"), "chamomile": (["SUMMER"], "Jun to Aug"),
    "catmint": (["SUMMER"], "May to Sep"), "snapdragon": (["SUMMER"], "Jun to Sep"), "scabious": (["SUMMER"], "Jun to Sep"),
    "verbena": (["SUMMER", "AUTUMN"], "Jul to Oct"), "sedum": (["AUTUMN"], "Aug to Oct"), "aster": (["AUTUMN"], "Sep to Oct"),
    "heather": (["WINTER"], "Dec to Mar"), "cranesbill": (["SUMMER"], "May to Sep"), "honesty": (["SPRING"], "Apr to May"),
    "sweetwilliam": (["SUMMER"], "Jun to Jul"), "wildgarlic": (["SPRING"], "Apr to May"),
    "strawberry": (["SPRING"], "May to Jun"), "raspberry": (["SUMMER"], "Jun"), "blackcurrant": (["SPRING"], "Apr"),
    "gooseberry": (["SPRING"], "Apr"), "redcurrant": (["SPRING"], "Apr"), "blueberry": (["SPRING"], "Apr to May"),
    "apple": (["SPRING"], "Apr to May"), "pear": (["SPRING"], "Apr"), "plum": (["SPRING"], "Mar to Apr"),
    "cherry": (["SPRING"], "Apr"), "blackberry": (["SUMMER"], "Jun to Jul"), "tayberry": (["SUMMER"], "May to Jun"),
    "broadbean": (["SPRING", "SUMMER"], "May to Jun"), "runnerbean": (["SUMMER"], "Jul to Sep"),
    "courgette": (["SUMMER"], "Jun to Sep"), "pumpkin": (["SUMMER"], "Jul to Aug"), "squash": (["SUMMER"], "Jul to Aug"),
    "tulip": (["SPRING"], "Apr to May"), "daffodil": (["SPRING"], "Mar to Apr"), "gladiolus": (["SUMMER"], "Jul to Sep"),
}

ALLIUMS = ["onion", "shallot", "garlic", "leek", "scallion", "chives"]
LEGUMES = ["pea", "mangetout", "broadbean", "runnerbean", "frenchbean"]
BRASSICAS = ["cabbage", "kale", "broccoli", "psb", "cauliflower", "sprouts", "swede", "turnip", "kohlrabi", "pakchoi"]


def P(id, name, emoji, cat, fam, dth, hw, **k):
    k.update(id=id, name=name, emoji=emoji, category=cat, family=fam, daysToHarvest=dth, harvestWeeks=hw)
    PLANTS.append(k)


def SOAK(what, extra=None, days=1):
    steps = [f"Put the {what} seeds in a cup of lukewarm water and leave them overnight."]
    steps += extra or []
    steps.append("Sow them the next day while they're still plump. Don't let them dry out again.")
    return dict(kind="SOAK", leadDays=days, title=f"Soak {what} seeds", steps=steps)


def CHIT():
    return dict(kind="CHIT", leadDays=42, title="Chit your seed potatoes", steps=[
        "Stand the seed potatoes in egg boxes with the end that has the most eyes facing up.",
        "Keep them somewhere bright, cool and frost free like a windowsill, porch or shed with a window.",
        "Leave them about six weeks. They're ready when the shoots are short, sturdy and about 2 cm long.",
        "Long pale shoots mean they're too dark. Move them somewhere brighter.",
    ])


def CHILL(what, weeks=4):
    return dict(kind="CHILL", leadDays=weeks * 7, title=f"Chill {what} seeds in the fridge", steps=[
        f"{what.capitalize()} seed needs a cold spell before it will germinate.",
        "Mix the seed with a handful of damp (not wet) compost or sand in a sealed freezer bag.",
        "Label it and put it in the fridge, not the freezer.",
        f"Leave it for {weeks} weeks, checking now and then that it hasn't dried out.",
        "Sow the seed and compost mix together when the time comes.",
    ])



# ---------------------------------------------------------------------
# Seed saving. Rules by plant type, with per-plant notes below.
# timing: HARVEST = save from ripe fruit while picking, END = leave the last ones to dry on the plant,
# NEXT_YEAR = biennial, flowers and seeds the year after, NONE = grow new plants another way.
F1_NOTE = "Only save seed from open-pollinated or heritage varieties. F1 hybrids don't come true from seed."

SEED_OVERRIDES = {
    "tomato": dict(difficulty="Easy", timing="HARVEST", years=4, steps=[
        "Pick a really ripe fruit from your best plant.",
        "Squeeze the seeds and jelly into a jar with a splash of water.",
        "Leave it on a windowsill for three days until a mould forms on top. This removes the gel and kills some diseases.",
        "Rinse the seeds in a sieve until clean, then spread them on a plate (not kitchen paper, they stick).",
        "Dry for a week, then store in a labelled paper envelope."]),
    "cherrytomato": "tomato",
    "pepper": dict(difficulty="Easy", timing="HARVEST", years=2, steps=[
        "Leave a fruit on the plant until it's fully coloured and starting to soften.",
        "Cut it open and scrape the seeds onto a plate.",
        "Dry for one to two weeks somewhere warm and airy, then store in an envelope."],
        note="Peppers and chillies can cross if grown close together. Grow just one variety if you want it true."),
    "chilli": "pepper", "aubergine": "pepper", "capegooseberry": "tomato", "tomatillo": "tomato",
    "cucumber": dict(difficulty="Moderate", timing="HARVEST", years=5, steps=[
        "Leave one fruit on the plant until it turns yellow and goes soft, well past eating stage.",
        "Scoop out the seeds into a jar of water and leave for two days to ferment, like tomatoes.",
        "Rinse, keep the seeds that sink and dry them on a plate for a week."],
        note="All-female and F1 cucumbers won't come true. Varieties cross easily so grow one at a time."),
    "courgette": dict(difficulty="Moderate", timing="HARVEST", years=5, steps=[
        "Pick a female flower that's about to open, tape it shut, then next morning hand pollinate it with a male flower of the same variety and tape it shut again.",
        "Tie a label on it and let that courgette grow into a big marrow.",
        "Leave it on the plant until the skin is hard, then store it for three more weeks.",
        "Scoop out the seeds, rinse and dry them."],
        note="Courgettes, marrows, pumpkins and squash cross with each other, so hand pollinating is the only way to be sure."),
    "marrow": "courgette", "pumpkin": "courgette", "squash": "courgette", "melon": "cucumber", "cucamelon": "cucumber",
    "pea": dict(difficulty="Easy", timing="END", years=3, steps=[
        "Leave a few of the best plants unpicked at the end of the season.",
        "Let the pods dry on the plant until they rattle.",
        "Pick, shell and dry the seeds indoors for another week.",
        "Store in a jar or envelope somewhere cool and dry."],
        note="Peas rarely cross so you can grow a few varieties side by side."),
    "mangetout": "pea", "broadbean": "pea", "frenchbean": "pea", "borlotti": "pea",
    "runnerbean": dict(difficulty="Easy", timing="END", years=3, steps=[
        "Leave a few pods at the top to swell and dry on the plant.",
        "Pick before hard frost and finish drying indoors until the pods are papery.",
        "Shell the beans and dry them for another week before storing."],
        note="Bees can cross runner bean varieties, so grow one variety for pure seed."),
    "lettuce": dict(difficulty="Easy", timing="END", years=3, steps=[
        "Leave one or two of your best plants to bolt and flower.",
        "When the flowers turn into fluffy white heads like dandelions, cut the whole stem.",
        "Shake or rub the heads into a paper bag and pick out the bits.",
        "Store the seed in a labelled envelope."],
        note="Lettuce pollinates itself so it rarely crosses."),
    "endive": "lettuce", "radicchio": "lettuce",
    "radish": dict(difficulty="Moderate", timing="END", years=4, steps=[
        "Leave a few radishes in the ground to flower.",
        "Let the pods go brown and crisp on the plant.",
        "Pick, crack open and store the seeds."],
        note="Radishes cross with each other, so grow one variety if you're saving seed."),
    "salad": "radish", "orientals": "radish", "pakchoi": "radish", "mooli": "radish", "landcress": "radish",
    "sweetcorn": dict(difficulty="Hard", timing="END", years=2, steps=[
        "Leave a few of the best cobs on the plant until the husks are dry and brown.",
        "Bring them in, peel back the husks and hang them somewhere airy to dry fully.",
        "Rub the kernels off and store them."],
        note="Sweetcorn is wind pollinated and needs a block of at least 50 plants for healthy seed. It also crosses with any other corn nearby."),
    "basil": dict(difficulty="Easy", timing="END", years=4, steps=[
        "Let a few plants flower at the end of summer.",
        "When the flower spikes go brown and dry, cut them into a paper bag.",
        "Crumble them to release the tiny black seeds and store them."]),
    "coriander": dict(difficulty="Easy", timing="END", years=3, steps=[
        "Let a few plants flower and set seed.",
        "When the round seeds turn brown, cut the stems into a paper bag and hang upside down to finish drying.",
        "Keep some to sow and use the rest in cooking."]),
    "dill": "coriander", "chervil": "coriander", "fennel": "coriander",
    "earlypotato": dict(difficulty="Not recommended", timing="NONE", years=0, steps=[
        "Buy fresh certified seed potatoes each year.",
        "Home-saved tubers carry viruses and blight that build up and shrink your crop."]),
    "maincrop": "earlypotato",
    "garlic": dict(difficulty="Easy", timing="END", years=1, steps=[
        "Garlic isn't grown from seed. Keep a few of your biggest, healthiest bulbs back after drying.",
        "Store them somewhere cool and airy and plant the biggest cloves in autumn."]),
    "elephantgarlic": "garlic", "shallot": "garlic",
    "onion": dict(difficulty="Hard", timing="NEXT_YEAR", years=1, steps=[
        "Onions flower in their second year. Replant a few good stored onions in spring.",
        "When the big round flower heads go dry in summer, cut them into a paper bag.",
        "Shake out the black seeds. Onion seed only keeps for a year, so sow it the next spring."],
        note="Most people just buy onion sets. Saving seed is a fun project rather than a money saver."),
    "redonion": "onion", "leek": "onion",
    "strawberry": dict(difficulty="Easy", timing="END", years=0, steps=[
        "Strawberries are grown from runners rather than seed.",
        "In summer, peg a few runners from healthy plants into small pots of compost.",
        "When they've rooted (about four weeks), snip them from the parent plant and you have free new plants."]),
    "rhubarb": dict(difficulty="Easy", timing="NONE", years=0, steps=[
        "Rhubarb doesn't come true from seed. Divide an old crown in winter with a spade instead.",
        "Each piece with a bud replants as a new plant."]),
    "mint": dict(difficulty="Easy", timing="NONE", years=0, steps=[
        "Mint is easiest from cuttings. Pull a piece with roots from the edge of the clump any time and pot it up."]),
    "asparagus": "rhubarb", "artichoke": "rhubarb", "jerusalem": "garlic", "horseradish": "rhubarb",
    "sweetpotato": dict(difficulty="Not practical", timing="NONE", years=0, steps=[
        "Sweet potatoes rarely flower in Ireland. Buy fresh slips each spring."]),
}

TREE_IDS = {"apple", "pear", "plum", "cherry", "fig", "grape"}
BUSH_IDS = {"blackcurrant", "redcurrant", "gooseberry", "blueberry", "raspberry", "autumnrasp", "blackberry", "tayberry"}
BIENNIALS = {"carrot", "parsnip", "beetroot", "chard", "leafbeet", "cabbage", "kale", "broccoli", "psb", "cauliflower",
             "sprouts", "swede", "turnip", "kohlrabi", "celery", "celeriac", "parsley", "savoy", "redcabbage",
             "cavolonero", "romanesco", "springcabbage", "salsify"}


def seed_info(p):
    pid = p["id"]
    o = SEED_OVERRIDES.get(pid)
    while isinstance(o, str):
        o = SEED_OVERRIDES.get(o)
    if o:
        d = dict(o)
    elif pid in TREE_IDS:
        d = dict(difficulty="Not practical", timing="NONE", years=0, steps=[
            "Fruit trees and vines don't come true from seed, and seedlings take years to fruit.",
            "New trees are grafted. Buy a named variety, or try hardwood cuttings for figs and grapes in winter."])
    elif pid in BUSH_IDS:
        d = dict(difficulty="Easy", timing="NONE", years=0, steps=[
            "Grow new bushes from cuttings rather than seed.",
            "In late autumn cut a pencil-thick stem about 25 cm long from this year's growth.",
            "Push it two thirds deep into a pot of gritty compost or a spare bit of ground and leave it until next autumn."])
    elif p["category"] == "BULB":
        d = dict(difficulty="Easy", timing="NONE", years=0, steps=[
            "Bulbs multiply by themselves. Every few years, lift a crowded clump after the leaves die back.",
            "Pull apart the little offset bulbs and replant them at the right depth. They'll flower in a year or two."])
    elif p.get("planted") and p.get("perennial"):
        d = dict(difficulty="Easy", timing="NONE", years=0, steps=[
            "The easiest way to get more is to divide the plant in spring or autumn.",
            "Dig it up, split it into pieces each with roots and shoots, and replant."])
    elif pid in BIENNIALS:
        d = dict(difficulty="Hard", timing="NEXT_YEAR", years=3, steps=[
            "This plant flowers in its second year. Leave a few of your best plants in the ground over winter, or replant them in spring.",
            "Let them flower in summer. Stake the flower stems as they get tall.",
            "When the seed heads turn brown and dry, cut them into a paper bag and hang them up to finish drying.",
            "Rub the seed out, pick out the bits and store in labelled envelopes."],
            note="Plants in this family cross easily with each other (and carrots with wild carrot). Keep only one variety flowering at a time.")
    else:
        # annual flowers and herbs that seed readily
        d = dict(difficulty="Easy", timing="END", years=3, steps=[
            "Leave a few of the best flowers on the plant instead of deadheading them.",
            "Wait until the seed heads are brown and dry. Collect on a dry afternoon.",
            "Snip them into a paper bag and leave somewhere airy for a week.",
            "Shake out the seeds, pick out the chaff and store in a labelled envelope somewhere cool and dry."])
    note = d.get("note", "")
    if d["timing"] in ("HARVEST", "END", "NEXT_YEAR"):
        note = (note + " " + F1_NOTE).strip()
    title = {"NONE": "How to grow more", "NEXT_YEAR": "Saving seed next year"}.get(d["timing"], "How to save seeds")
    return dict(difficulty=d["difficulty"], timing=d["timing"], storeYears=d["years"], note=note,
                section=dict(title=title, facts=([["Difficulty", d["difficulty"]]] +
                             ([["Seed keeps", f"about {d['years']} year{'s' if d['years'] != 1 else ''}"]] if d["years"] else [])),
                             steps=d["steps"]))


HARDEN_STEPS = [
    "About a week before planting out, start getting the plants used to life outside.",
    "Days 1 to 2. Put them out in a sheltered, shady spot for two or three hours and bring them back in.",
    "Days 3 to 5. Leave them out most of the day in brighter light, back in at night.",
    "Days 6 to 7. Leave them out day and night unless frost is forecast.",
    "Going into the tunnel? A few days sitting in the tunnel before planting is enough.",
    "Water them well while they're outside. Pots dry out fast in the wind.",
]


def build(p):
    planted = p.get("planted", False)
    indoor = p.get("sowIndoorsWeeksBefore") is not None
    space, row = p.get("space", 30), p.get("row", 30)
    depth = p.get("depth", "1 cm")
    germ = p.get("germ")
    layout = p.get("layout") or f"Rows {row} cm apart with plants {space} cm apart along each row."
    facts, steps = [], []

    if planted:
        title = "How to plant"
        facts = [["Depth", depth], ["Spacing", f"{space} cm"], ["Rows", f"{row} cm apart"]]
        if p.get("potOnly"):
            facts = [["Depth", depth], ["Pot", p.get("potSize", "a large pot")]]
        steps = [] if p.get("potOnly") else ["Fork over the bed, pull out weeds and work in a bucket of compost per square metre."]
        steps += p["planting"]
        steps.append("Water in well, even if rain is forecast. It settles the soil around the roots.")
        sow = dict(title=title, facts=facts, steps=steps, layout=layout)
    elif indoor:
        pot = p.get("pot", "modules or small pots")
        seeds = p.get("seeds", 2)
        facts = [["Pot", pot], ["Seeds per pot", str(seeds)], ["Depth", depth]]
        if germ:
            facts.append(["Comes up in", germ])
        steps = [f"Fill {pot} with seed compost, tap it level and water it before sowing."]
        s = str(seeds)
        if not s[0].isdigit():
            if depth.startswith("surface"):
                steps.append(f"Sprinkle {s} of seed thinly over the surface and press it in lightly. Barely cover it, the seed needs light.")
            else:
                steps.append(f"Sprinkle {s} of seed thinly over the surface and cover with {depth} of compost.")
        elif depth.startswith("surface"):
            steps.append(f"Sow {s} seed{'s' if s != '1' else ''} per pot on the surface and press them in lightly. Barely cover them, they need light.")
        else:
            steps.append(f"Sow {s} seed{'s' if s != '1' else ''} per pot {depth} deep and cover with compost.")
        steps += p.get("sow_extra", [])
        steps.append(p.get("warmth", "Keep them on a bright windowsill or in a propagator.")
                     + (f" Seedlings should appear in {germ}." if germ else ""))
        if str(seeds) not in ("1",) and not p.get("clump"):
            steps.append("When the first true leaves open, keep the strongest seedling and snip the rest off at soil level.")
        if p.get("pot_on"):
            steps.append("Pot on into a 9 cm pot when roots start poking out the bottom.")
        steps.append("Keep the compost damp but never soggy and give them as much light as you can so they don't go leggy.")
        sow = dict(title="How to sow indoors", facts=facts, steps=steps)
    else:
        facts = [["Depth", depth], ["Spacing", f"{space} cm"], ["Rows", f"{row} cm apart"]]
        if germ:
            facts.append(["Comes up in", germ])
        steps = ["Rake the soil to a fine crumb and pull out any weeds and big stones."]
        if p.get("station"):
            steps.append(f"Sow {p.get('seeds', 2)} seeds at each spot, {space} cm apart and {depth} deep.")
        elif p.get("broadcast"):
            steps.append("Scatter the seed thinly over the bed and rake it in lightly.")
        else:
            steps.append(f"Use the edge of a hoe or a cane to make drills {depth} deep and {row} cm apart.")
            steps.append("Sprinkle the seed thinly along the drill.")
        steps += p.get("sow_extra", [])
        if not p.get("broadcast"):
            steps.append("Cover with soil, firm gently with the back of the rake and water with a fine rose.")
        if p.get("thin", True) and not p.get("broadcast"):
            steps.append(f"When seedlings are big enough to handle, thin them to one every {space} cm.")
        sow = dict(title="How to sow", facts=facts, steps=steps, layout=layout)

    plant_out = None
    harden = None
    if indoor:
        harden = dict(title="Harden off", facts=[], steps=HARDEN_STEPS)
        po = ["Water the plants an hour before moving them so the rootball holds together."]
        if p.get("feed") == "HIGH":
            po.append("This is a hungry crop. Dig in a bucket of compost or well-rotted manure first.")
        po.append(p.get("plant_depth", "Dig a hole a little bigger than the pot and set the plant at the same depth it was growing."))
        po.append(f"Space them {space} cm apart with rows {row} cm apart.")
        po += p.get("plant_extra", [])
        po.append("Firm the soil gently around each plant and water in well.")
        plant_out = dict(title="How to plant out", facts=[["Spacing", f"{space} cm"], ["Rows", f"{row} cm apart"]],
                         steps=po, layout=layout)

    care = list(p.get("care", []))
    if p.get("water") == "HIGH":
        care.append("Thirsty plant. In dry spells give it a deep soak two or three times a week rather than a sprinkle every day.")
    elif p.get("water") == "LOW":
        care.append("Only needs watering in long dry spells.")
    if p.get("feed") == "HIGH" and not any("feed" in c.lower() for c in care):
        care.append("Hungry plant. A liquid feed every two weeks in summer gives a better crop.")
    if not p.get("potOnly"):
        care.append("Keep the bed weeded, especially while plants are small.")

    harvest = p.get("harvest") or ["Harvest when ready and enjoy."]

    guide = dict(spacingCm=space, rowCm=row, sow=sow, harden=harden, plantOut=plant_out, care=care, harvest=harvest,
                 seedSaving=seed_info(p))
    if p.get("prep"):
        pr = p["prep"]
        guide["prep"] = dict(kind=pr["kind"], leadDays=pr["leadDays"],
                             section=dict(title=pr["title"], facts=[], steps=pr["steps"]))

    out = {k: p[k] for k in ["id", "name", "emoji", "category", "family", "daysToHarvest", "harvestWeeks"]}
    p.setdefault("potOk", p["id"] not in NOT_IN_POTS)
    if p["potOk"] and "potSize" not in p:
        sp = p.get("space", 30)
        p["potSize"] = ("15 cm pot or window box" if sp <= 10 else "20 to 30 cm pot" if sp <= 30
                        else "30 to 40 cm pot" if sp <= 60 else "large tub, 50 litres or more")
    if p["id"] in POLLINATORS and "bloom" not in p:
        p["bloom"], p["flowers"] = POLLINATORS[p["id"]]
    if p.get("bloom"):
        p["pollinator"] = True
    for k in ["sowIndoorsWeeksBefore", "plantOutWeeksAfter", "directWeeksAfter", "fixedSow", "frostHardy", "tunnel",
              "water", "feed", "perennial", "planted", "solo", "companions", "avoid", "tip",
              "potOk", "potSize", "pollinator", "bloom", "flowers"]:
        if k in p:
            out[k] = p[k]
    out["guide"] = guide
    return out


# =====================================================================
# VEG
# =====================================================================
P("tomato", "Tomato", "🍅", "VEG", "SOLANACEAE", 70, 12, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=2,
  tunnel="NEEDS", water="HIGH", feed="HIGH", companions=["basil", "marigold", "chives", "borage", "garlic"],
  avoid=["earlypotato", "maincrop", "fennel"] + BRASSICAS,
  tip="Cordon types need side shoots pinched out. Bush types don't.",
  pot="small pots or modules", seeds=2, depth="0.5 cm", germ="7 to 14 days at 18 to 24°C", pot_on=True,
  warmth="Keep them warm, ideally 18 to 24°C, on a sunny windowsill or in a propagator.",
  space=45, row=60, feed_note=True,
  layout="Two staggered rows along the sunniest side of the bed, each plant with a cane or a string tied to the frame above.",
  plant_depth="Plant them deeper than they were in the pot, up to the first leaves. Tomatoes grow extra roots along the buried stem.",
  plant_extra=["Tie each plant loosely to a cane, or twist it round a string tied to the tunnel frame."],
  care=["Pinch out side shoots on cordon types every week. They grow in the joint between leaf and stem.",
        "Water little and often at the same time each day. Uneven watering causes split fruit and blossom end rot.",
        "Feed weekly with tomato feed once the first truss of fruit has set.",
        "When there are six or seven trusses, pinch out the top so the plant ripens what it has."],
  harvest=["Pick when fully coloured and just slightly soft.",
           "At the end of the season ripen green fruit indoors in a bag with a banana."])

P("pepper", "Sweet pepper", "🫑", "VEG", "SOLANACEAE", 80, 8, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=3,
  tunnel="NEEDS", feed="HIGH", companions=["basil", "marigold"], tip="Slow to start so sow early somewhere warm.",
  pot="modules", seeds=2, depth="0.5 cm", germ="10 to 21 days at 21 to 27°C", pot_on=True,
  warmth="Peppers need real warmth to germinate. A heated propagator or the top of a warm press works well.",
  space=45, row=50, layout="A single or staggered row in the warmest part of the tunnel.",
  plant_extra=["Push in a short cane beside each plant. It'll need support once fruit gets heavy."],
  care=["Pinch out the very first flower to get a bushier, stronger plant.",
        "Feed weekly with tomato feed once flowers appear.", "Mist the plants on hot days to help fruit set."],
  harvest=["Pick green, or leave them to turn red, yellow or orange for a sweeter taste.",
           "Cut with scissors rather than pulling so you don't snap the branch."])

P("chilli", "Chilli", "🌶️", "VEG", "SOLANACEAE", 90, 10, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=3,
  tunnel="NEEDS", feed="HIGH", companions=["basil", "marigold"], tip="Loves heat. A sunny tunnel corner is perfect.",
  pot="modules", seeds=2, depth="0.5 cm", germ="10 to 21 days at 21 to 27°C", pot_on=True,
  warmth="Chillies need warmth to germinate. Use a heated propagator or a warm spot like the top of the hot press.",
  space=40, row=45, layout="A single row along the warmest edge of the tunnel bed, or in big pots.",
  care=["Let the compost dry a little between waterings for hotter chillies.",
        "Feed weekly with tomato feed once flowers appear."],
  harvest=["Pick regularly to keep more coming.", "Thread spares on a string and dry them in a warm room."])

P("aubergine", "Aubergine", "🍆", "VEG", "SOLANACEAE", 90, 6, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=3,
  tunnel="NEEDS", water="HIGH", feed="HIGH", companions=["marigold", "basil"],
  tip="Needs the warmest spot you have.",
  pot="modules", seeds=2, depth="0.5 cm", germ="10 to 21 days at 21 to 25°C", pot_on=True,
  warmth="Aubergines need warmth to germinate. A heated propagator is best.",
  space=60, row=60, layout="A single row in the warmest part of the tunnel with a cane for each plant.",
  plant_extra=["Put a cane in beside each plant."],
  care=["Pinch out the growing tip at 30 cm tall for a bushy plant.",
        "Keep only five or six fruits per plant so they grow to a good size.",
        "Mist the flowers on hot days and feed weekly with tomato feed."],
  harvest=["Pick while the skin is glossy. Dull skin means it's past its best."])

P("cucumber", "Cucumber", "🥒", "VEG", "CUCURBIT", 50, 8, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3,
  tunnel="NEEDS", water="HIGH", feed="HIGH", companions=["dill", "nasturtium", "frenchbean", "pea"],
  avoid=["earlypotato", "maincrop", "sage"], tip="All-female varieties give more fruit and are less bitter.",
  pot="9 cm pots", seeds=1, depth="1 cm", germ="4 to 7 days at 20 to 25°C",
  sow_extra=["Push the seed in on its edge so water doesn't sit on the flat side and rot it."],
  warmth="Keep warm, about 20 to 25°C.",
  space=60, row=90, layout="One row along the back of the bed with strings or netting for them to climb.",
  plant_depth="Plant with the top of the rootball sitting slightly above the soil. Cucumber stems rot if buried.",
  plant_extra=["Give each plant a string or cane to climb up."],
  care=["Remove any male flowers on all-female varieties (no baby cucumber behind the flower).",
        "Pinch out side shoots two leaves past a female flower.",
        "Water every day in warm weather and feed every two weeks."],
  harvest=["Pick when about 30 cm long or the size it says on the packet.",
           "Keep picking or the plant stops producing."])

P("courgette", "Courgette", "🥒", "VEG", "CUCURBIT", 50, 10, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3,
  water="HIGH", feed="HIGH", companions=["nasturtium", "sweetcorn", "runnerbean", "borage"],
  avoid=["earlypotato", "maincrop"], tip="One or two plants is plenty for most families.",
  pot="9 cm pots", seeds=1, depth="2 cm", germ="5 to 10 days at 18 to 25°C",
  sow_extra=["Sow the seed on its edge so it doesn't rot."],
  space=90, row=90, layout="One or two plants per bed with lots of room around each. They get big.",
  plant_depth="Plant into a slight hollow so water collects around the roots, not the stem.",
  plant_extra=["Protect young plants from slugs for the first couple of weeks."],
  care=["Water at the base, never over the leaves, to avoid powdery mildew.",
        "Feed every 10 to 14 days once fruits start."],
  harvest=["Pick at 10 to 15 cm long. Check every couple of days in summer or you'll get marrows.",
           "The flowers are edible too. Try them stuffed or fried."])

P("squash", "Butternut squash", "🎃", "VEG", "CUCURBIT", 110, 3, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3,
  tunnel="LIKES", water="HIGH", feed="HIGH", companions=["sweetcorn", "runnerbean", "nasturtium"],
  avoid=["earlypotato", "maincrop"], tip="Ripens far better in a tunnel in an Irish summer.",
  pot="9 cm pots", seeds=1, depth="2 cm", germ="5 to 10 days at 20 to 25°C",
  sow_extra=["Sow the seed on its edge so it doesn't rot."],
  space=100, row=100, layout="One or two plants per bed, vines trained along the bed edge or up a strong frame.",
  plant_depth="Plant onto a low mound of compost so water drains away from the stem.",
  care=["Train the vines where you want them to go.", "Stop each vine once two or three fruits have set."],
  harvest=["Leave on the plant until the skin is hard and the stalk has dried.",
           "Cure them somewhere warm and sunny for two weeks and they'll store all winter."])

P("pumpkin", "Pumpkin", "🎃", "VEG", "CUCURBIT", 100, 3, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3,
  tunnel="OUTDOOR", water="HIGH", feed="HIGH", companions=["sweetcorn", "runnerbean", "nasturtium"],
  avoid=["earlypotato", "maincrop"], tip="Needs lots of room. One plant can spread three metres.",
  pot="9 cm pots", seeds=1, depth="2 cm", germ="5 to 10 days at 20 to 25°C",
  sow_extra=["Sow the seed on its edge so it doesn't rot."],
  space=120, row=150, layout="One plant per bed or at the end of a bed where it can sprawl onto a path.",
  plant_depth="Plant on a mound of compost or well-rotted manure.",
  care=["Slide a tile or piece of wood under each pumpkin to stop it rotting.",
        "Water heavily once a week rather than a little every day."],
  harvest=["Cut when the skin is hard and sounds hollow when knocked, leaving a long stalk.",
           "Cure in the sun for a week or two before storing."])

P("sweetcorn", "Sweetcorn", "🌽", "VEG", "OTHER", 85, 3, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3,
  companions=["runnerbean", "pumpkin", "courgette", "frenchbean", "squash"],
  tip="Choose an early variety for Irish summers.",
  pot="deep modules or root trainers", seeds=1, depth="2.5 cm", germ="7 to 10 days at 18 to 25°C",
  space=40, row=40, layout="A square block of plants 40 cm each way, never a single row. The wind needs to blow pollen from plant to plant.",
  plant_extra=["Handle them gently. Sweetcorn hates having its roots disturbed."],
  care=["Draw a little soil up round the stems to stop them blowing over.", "Water well once the tassels appear."],
  harvest=["Ready when the silky tassels turn brown and a kernel pressed with your nail gives milky juice.",
           "Cook within hours of picking for the sweetest taste."])

P("earlypotato", "Early potatoes", "🥔", "VEG", "SOLANACEAE", 90, 6, directWeeksAfter=-4, tunnel="OUTDOOR",
  feed="HIGH", planted=True, companions=["broadbean", "pea", "marigold", "nasturtium", "calendula"],
  avoid=["tomato", "cucumber", "courgette", "pumpkin", "squash", "sunflower", "melon"],
  tip="Classic Irish earlies like Home Guard are ready by late June.",
  prep=CHIT(), depth="12 cm", space=30, row=60,
  planting=["Dig a trench or individual holes 12 cm deep.",
            "Set each potato in with the shoots facing up, taking care not to snap them.",
            "Space them 30 cm apart in rows 60 cm apart.", "Cover gently with soil."],
  layout="Straight rows across the bed, 60 cm apart, so there's room to draw soil up the stems later.",
  care=["When stems are 20 cm tall, earth them up by drawing soil over them so just the tips show.",
        "If frost is forecast, cover any shoots with soil or fleece.",
        "Water well once flowers appear. That's when the potatoes swell."],
  harvest=["Earlies are usually ready around flowering time. Scrape away some soil to check the size.",
           "Dig in from the side of the row with a fork so you don't spear them."])

P("maincrop", "Maincrop potatoes", "🥔", "VEG", "SOLANACEAE", 130, 8, directWeeksAfter=0, tunnel="OUTDOOR",
  feed="HIGH", planted=True, companions=["broadbean", "marigold", "nasturtium", "calendula"],
  avoid=["tomato", "cucumber", "courgette", "pumpkin", "squash", "sunflower", "melon"],
  tip="Blight-resistant varieties like Sarpo Mira save a lot of heartache in wet summers.",
  prep=CHIT(), depth="12 cm", space=38, row=75,
  planting=["Dig a trench or holes 12 cm deep.", "Set each potato in with the shoots facing up.",
            "Space them 38 cm apart in rows 75 cm apart.", "Cover gently with soil."],
  layout="Straight rows 75 cm apart across the bed for earthing up.",
  care=["Earth up the stems when they're 20 cm tall and again three weeks later.",
        "Watch for blight in warm, wet weather (brown patches on leaves). Cut off and bin affected foliage straight away.",
        "Water in long dry spells once flowering starts."],
  harvest=["When the foliage dies back, cut it off and wait two weeks so the skins harden.",
           "Lift on a dry day, leave them on the soil for a few hours to dry and store in paper sacks somewhere dark and frost free."])

P("carrot", "Carrot", "🥕", "VEG", "UMBELLIFER", 75, 10, directWeeksAfter=-3, frostHardy=True, water="LOW",
  feed="LOW", companions=["onion", "leek", "chives", "scallion", "lettuce", "pea", "radish"], avoid=["dill"],
  tip="Stony or freshly manured soil makes forked carrots.",
  depth="1 cm", space=5, row=20, germ="14 to 21 days. Slow, so keep the drill damp",
  sow_extra=["The seed is tiny. Mix it with a little dry sand so you can sow it thinly."],
  layout="Short rows across the bed 20 cm apart. A 60 cm fleece fence or a row of onions alongside helps keep carrot fly away.",
  care=["Thin on a calm evening and pinch off thinnings. The smell attracts carrot fly.",
        "Weed by hand. Carrots hate competition."],
  harvest=["Pull when the orange shoulders show and are the width you like.",
           "Water the row first to make pulling easier."])

P("parsnip", "Parsnip", "🥕", "VEG", "UMBELLIFER", 120, 14, directWeeksAfter=-3, frostHardy=True, tunnel="OUTDOOR",
  water="LOW", feed="LOW", companions=["onion", "garlic", "radish"], tip="Always buy fresh seed. Old seed rarely comes up.",
  prep=SOAK("parsnip", ["It softens the seed coat and speeds up their very slow germination."]),
  station=True, seeds=3, depth="1.5 cm", space=15, row=30, germ="21 to 28 days. Very slow, be patient",
  sow_extra=["Sow a few radish seeds along the row too. They come up fast and mark where the parsnips are."],
  layout="Rows 30 cm apart with a plant every 15 cm.",
  care=["Thin to the strongest seedling at each spot."],
  harvest=["Lift from October once the leaves die back.", "They taste sweeter after a frost so leave some in the ground over winter."])

P("beetroot", "Beetroot", "🟣", "VEG", "AMARANTH", 70, 10, directWeeksAfter=-2, frostHardy=True,
  companions=["onion", "lettuce", "garlic"] + BRASSICAS, avoid=["runnerbean"],
  tip="Boltardy is a reliable variety for early sowings.",
  prep=SOAK("beetroot", ["This softens the corky seed casing."]),
  depth="2.5 cm", space=10, row=30, germ="10 to 14 days",
  sow_extra=["Each 'seed' is actually a cluster of a few seeds, so sow them 5 cm apart."],
  layout="Rows 30 cm apart with a beet every 10 cm.",
  care=["Thin to one seedling every 10 cm. Eat the thinnings as baby salad leaves."],
  harvest=["Pull at golf ball to tennis ball size for the best flavour.",
           "Twist the leaves off rather than cutting so they don't bleed."])

P("radish", "Radish", "🔴", "VEG", "BRASSICA", 28, 3, directWeeksAfter=-4, frostHardy=True, tunnel="LIKES",
  feed="LOW", companions=["carrot", "lettuce", "pea", "parsnip", "spinach"],
  tip="Sow a pinch every two weeks for a steady supply.",
  depth="1 cm", space=3, row=15, germ="4 to 7 days",
  layout="Short rows anywhere there's a gap, even between slower crops like parsnips.",
  care=["Keep them moist or they go woody and hot."],
  harvest=["Pull about four weeks after sowing while still small and crisp."])

P("lettuce", "Lettuce", "🥗", "VEG", "DAISY", 45, 8, sowIndoorsWeeksBefore=3, plantOutWeeksAfter=-1,
  frostHardy=True, tunnel="LIKES", feed="LOW", companions=["carrot", "radish", "beetroot", "chives", "strawberry", "onion"],
  tip="Sow little and often. Loose-leaf types keep cropping for weeks.",
  pot="modules", seeds=2, depth="surface", germ="4 to 10 days. Keep it below 20°C or it won't germinate",
  warmth="Keep them somewhere bright and cool. Lettuce won't germinate in heat.",
  space=25, row=25, layout="A grid 25 cm each way, or dot them into gaps between bigger crops.",
  plant_depth="Plant so the base of the leaves sits just above the soil. Buried stems rot.",
  care=["Protect from slugs, especially just after planting.", "Water in the morning in dry spells."],
  harvest=["Pick outer leaves as you need them or cut the whole head.",
           "Sow more every three weeks so you always have some coming."])

P("salad", "Salad leaves and rocket", "🌱", "VEG", "BRASSICA", 30, 8, directWeeksAfter=-2, frostHardy=True,
  tunnel="LIKES", feed="LOW", companions=["carrot", "radish"], tip="Great for the tunnel right through autumn and winter.",
  depth="1 cm", space=3, row=15, thin=False, germ="5 to 10 days",
  layout="Short rows 15 cm apart, or a wide band you can cut like a lawn.",
  care=["Keep watered to stop it running to seed."],
  harvest=["Cut 3 cm above the soil and it will grow back two or three times."])

P("spinach", "Spinach", "🍀", "VEG", "AMARANTH", 45, 6, directWeeksAfter=-4, frostHardy=True, tunnel="LIKES",
  companions=["strawberry", "radish", "pea"], tip="Bolts in hot dry weather so keep it watered.",
  depth="2 cm", space=15, row=30, germ="7 to 14 days",
  care=["Water well in dry weather to stop it bolting."],
  harvest=["Pick leaves young and tender.", "Sow again in late summer for an autumn crop."])

P("chard", "Rainbow chard", "🌈", "VEG", "AMARANTH", 60, 20, directWeeksAfter=0, frostHardy=True,
  companions=["onion"] + BRASSICAS, tip="One sowing can crop from summer right through winter.",
  prep=SOAK("chard"), depth="2 cm", space=30, row=40, germ="7 to 14 days",
  harvest=["Twist off the outer stems at the base and leave the middle to keep growing.",
           "Keeps cropping through winter, especially in the tunnel."])

P("leafbeet", "Perpetual spinach", "🍃", "VEG", "AMARANTH", 60, 30, directWeeksAfter=-1, frostHardy=True,
  companions=["onion"] + BRASSICAS, tip="Easier than true spinach and it doesn't bolt.",
  prep=SOAK("perpetual spinach"), depth="2 cm", space=30, row=40, germ="7 to 14 days",
  harvest=["Pick outer leaves regularly. One sowing crops for about a year."])

P("kale", "Kale", "🥬", "VEG", "BRASSICA", 70, 24, sowIndoorsWeeksBefore=0, plantOutWeeksAfter=6, frostHardy=True,
  tunnel="OUTDOOR", feed="HIGH", companions=["onion", "beetroot", "nasturtium", "dill", "chard", "calendula"],
  avoid=["strawberry", "tomato"], tip="Net against cabbage white butterflies and pigeons.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days",
  space=45, row=60, layout="Staggered rows with a net over the whole bed to keep off pigeons and butterflies.",
  plant_depth="Plant firmly, up to the first leaves. Brassicas need firm soil so tread around them.",
  plant_extra=["Fit a collar of card or carpet underlay around each stem to stop cabbage root fly."],
  care=["Keep the net on. Check under leaves for caterpillars and squash any eggs.",
        "Stake tall plants before the autumn gales."],
  harvest=["Pick leaves from the bottom up, leaving the top to keep growing.", "A touch of frost makes kale sweeter."])

P("cabbage", "Cabbage", "🥬", "VEG", "BRASSICA", 80, 6, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=1,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", companions=["onion", "beetroot", "nasturtium", "dill", "calendula"],
  avoid=["strawberry", "tomato"], tip="Firm soil is the secret to solid heads.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days",
  space=45, row=45, layout="A grid 45 cm each way under netting.",
  plant_depth="Plant firmly up to the first leaves and tread the soil around each one.",
  plant_extra=["Fit a collar around each stem against cabbage root fly."],
  care=["Keep netted against butterflies and pigeons."],
  harvest=["Cut when the head feels firm.", "Cut a cross in the leftover stump and it may give you a few small bonus heads."])

P("broccoli", "Broccoli (calabrese)", "🥦", "VEG", "BRASSICA", 70, 4, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=2,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", companions=["onion", "beetroot", "nasturtium", "dill"],
  avoid=["strawberry", "tomato"], tip="After the main head, side shoots keep coming.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days",
  space=30, row=45, layout="Rows 45 cm apart under netting.",
  plant_depth="Plant firmly up to the first leaves.",
  plant_extra=["Fit a collar around each stem against cabbage root fly."],
  care=["Keep netted and watered. Dry spells make heads small and loose."],
  harvest=["Cut the main head while the buds are still tight and green.", "Keep cutting the side shoots that follow."])

P("psb", "Purple sprouting broccoli", "🥦", "VEG", "BRASSICA", 240, 8, sowIndoorsWeeksBefore=0, plantOutWeeksAfter=6,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", companions=["onion", "beetroot", "nasturtium"],
  avoid=["strawberry", "tomato"], tip="Takes almost a year but crops in the hungry gap of early spring.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days",
  space=60, row=75, layout="Staggered rows 75 cm apart under netting. Plants grow over a metre tall.",
  plant_depth="Plant deeply and firmly up to the first leaves.",
  plant_extra=["Fit a collar around each stem against cabbage root fly."],
  care=["Stake plants before winter and firm the soil after storms."],
  harvest=["Snap off spears while the flower buds are still closed.", "Keep picking and more will come for weeks."])

P("cauliflower", "Cauliflower", "🤍", "VEG", "BRASSICA", 90, 3, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=1,
  frostHardy=True, tunnel="OUTDOOR", water="HIGH", feed="HIGH", companions=["onion", "beetroot", "nasturtium"],
  avoid=["strawberry", "tomato"], tip="The fussiest brassica. Never let it dry out.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days",
  space=60, row=60, layout="A grid 60 cm each way under netting.",
  plant_depth="Plant firmly up to the first leaves.",
  plant_extra=["Fit a collar around each stem against cabbage root fly."],
  care=["When a curd starts to form, fold a few leaves over it to keep it white."],
  harvest=["Cut while the curd is still tight and white."])

P("sprouts", "Brussels sprouts", "🟢", "VEG", "BRASSICA", 140, 14, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=5,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", companions=["onion", "nasturtium", "beetroot"],
  avoid=["strawberry", "tomato"], tip="Ready in time for the Christmas dinner.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days",
  space=60, row=75, layout="Staggered rows 75 cm apart under netting.",
  plant_depth="Plant deeply and very firmly. Loose soil gives loose, open sprouts.",
  plant_extra=["Fit a collar around each stem against cabbage root fly."],
  care=["Stake each plant before the autumn winds.", "Pull off yellowing lower leaves."],
  harvest=["Pick from the bottom of the stem upwards once sprouts are firm and walnut sized.",
           "The leafy top is a tasty bonus green at the end."])

P("swede", "Swede turnip", "🟡", "VEG", "BRASSICA", 150, 16, directWeeksAfter=2, frostHardy=True, tunnel="OUTDOOR",
  companions=["onion", "pea"], tip="The turnip of the Irish dinner plate.",
  depth="1.5 cm", space=25, row=40, germ="7 to 10 days",
  care=["Water in dry spells to stop the roots going woody."],
  harvest=["Lift from October as needed. They're happy left in the ground over winter."])

P("turnip", "White turnip", "⚪", "VEG", "BRASSICA", 50, 4, directWeeksAfter=-2, frostHardy=True,
  companions=["onion", "pea"], tip="Quick and tasty when picked small.",
  depth="1.5 cm", space=15, row=25, germ="5 to 10 days",
  harvest=["Pull at golf ball size when they're sweet and tender."])

P("kohlrabi", "Kohlrabi", "🔵", "VEG", "BRASSICA", 55, 4, directWeeksAfter=0, frostHardy=True,
  companions=["onion", "beetroot"], tip="Crunchy like an apple. Kids often like it raw.",
  depth="1.5 cm", space=20, row=30, germ="5 to 10 days",
  harvest=["Pick when the swollen stem is the size of a tennis ball or smaller."])

P("pakchoi", "Pak choi", "🥬", "VEG", "BRASSICA", 45, 4, directWeeksAfter=10, frostHardy=True, tunnel="LIKES",
  companions=["onion"], tip="Sow from mid summer. Earlier sowings tend to bolt.",
  depth="1 cm", space=20, row=30, germ="4 to 7 days",
  harvest=["Cut whole heads, or pick individual leaves as baby greens."])

P("orientals", "Oriental greens (mizuna, mustard)", "🥬", "VEG", "BRASSICA", 35, 12, directWeeksAfter=12,
  frostHardy=True, tunnel="LIKES", feed="LOW", tip="Superb winter salad in the tunnel.",
  depth="1 cm", space=15, row=20, germ="4 to 7 days",
  harvest=["Cut leaves as needed and they regrow."])

P("lambslettuce", "Lamb's lettuce", "🌱", "VEG", "OTHER", 60, 16, directWeeksAfter=14, frostHardy=True,
  tunnel="LIKES", feed="LOW", tip="Tough little winter salad.",
  depth="1 cm", space=10, row=15, germ="7 to 14 days",
  harvest=["Cut whole rosettes or pick leaves through autumn and winter."])

P("celery", "Celery", "🥬", "VEG", "UMBELLIFER", 90, 8, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=4,
  water="HIGH", feed="HIGH", companions=["leek", "cabbage"], tip="Self-blanching types are far less work.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="14 to 21 days at 15 to 20°C", clump=True,
  sow_extra=["When seedlings have two leaves, prick them out into modules, one per cell."],
  space=25, row=25, layout="A block 25 cm each way so the plants shade each other's stems.",
  care=["Never let it dry out or the stems go stringy."],
  harvest=["Cut whole plants at the base from late summer."])

P("celeriac", "Celeriac", "🥔", "VEG", "UMBELLIFER", 150, 12, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=4,
  water="HIGH", feed="HIGH", companions=["leek"], tip="Slow but stores well and tastes great in soups.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="14 to 21 days at 15 to 20°C", clump=True,
  sow_extra=["Prick seedlings out into modules when they have two leaves."],
  space=30, row=40,
  plant_depth="Plant with the crown at soil level, not buried.",
  care=["In late summer pull off lower leaves and side shoots to expose the bulb."],
  harvest=["Lift from October when the bulbs are 10 cm or more across."])

P("fennel", "Florence fennel", "🌿", "VEG", "UMBELLIFER", 70, 4, sowIndoorsWeeksBefore=-2, plantOutWeeksAfter=4,
  water="HIGH", avoid=["tomato"], tip="Hates root disturbance so sow in modules.",
  pot="modules", seeds=2, depth="1 cm", germ="7 to 14 days",
  space=30, row=30, layout="A grid 30 cm each way.",
  plant_extra=["Plant out before roots fill the module. Older plants tend to bolt."],
  care=["Water well. A dry spell will make it bolt."],
  harvest=["Cut the bulb just above the ground when it's the size of a tennis ball."])

P("onion", "Onion", "🧅", "VEG", "ALLIUM", 120, 4, directWeeksAfter=-4, frostHardy=True, water="LOW", feed="LOW",
  planted=True, companions=["carrot", "beetroot", "lettuce", "strawberry", "chard", "parsnip"] + BRASSICAS,
  avoid=LEGUMES, tip="Sets are far easier than seed.",
  depth="just the tip showing", space=10, row=30,
  planting=["Gently push each onion set into the soil so just the tip shows. Don't screw them in.",
            "Space them 10 cm apart in rows 30 cm apart."],
  layout="Neat rows across the bed. A row of onions makes a good border next to carrots.",
  care=["Check for a few weeks after planting and push back any that birds pull up.",
        "Hand weed carefully. Onions hate competition."],
  harvest=["When the leaves flop over and turn yellow, lift them and leave them to dry in the sun for a week.",
           "Store in nets somewhere cool and airy."])

P("shallot", "Shallot", "🧅", "VEG", "ALLIUM", 120, 3, directWeeksAfter=-8, frostHardy=True, water="LOW", feed="LOW",
  planted=True, companions=["carrot", "beetroot", "lettuce", "strawberry"], avoid=LEGUMES,
  tip="Each little bulb grows into a cluster of eight or more.",
  depth="just the tip showing", space=20, row=30,
  planting=["Push each shallot into the soil so the tip just shows.", "Space them 20 cm apart in rows 30 cm apart."],
  care=["Push back any that birds lift.", "Weed by hand."],
  harvest=["Lift in July once the leaves turn yellow.", "Dry them in the sun, then split the clusters and store."])

P("garlic", "Garlic", "🧄", "VEG", "ALLIUM", 260, 3, fixedSow=[10, 20], frostHardy=True, tunnel="OUTDOOR",
  water="LOW", feed="LOW", planted=True, companions=["carrot", "beetroot", "strawberry", "tomato", "parsnip"],
  avoid=LEGUMES, tip="It needs the winter cold to split into cloves.",
  depth="2 cm below the surface", space=15, row=30,
  planting=["Split the bulb into cloves just before planting. Use only the big outer cloves.",
            "Push each clove in pointy end up so the tip sits 2 cm below the surface.",
            "Space them 15 cm apart in rows 30 cm apart."],
  layout="Rows 30 cm apart across the bed.",
  care=["Keep weed free and water in dry spells in spring.",
        "If it sends up a curly flower stalk (a scape), snap it off and eat it."],
  harvest=["Lift in late June or July when about half the leaves have gone yellow.",
           "Dry somewhere sunny and airy for two weeks before storing."])

P("leek", "Leek", "🟩", "VEG", "ALLIUM", 110, 16, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=5, frostHardy=True,
  tunnel="OUTDOOR", companions=["carrot", "beetroot", "celery"], avoid=LEGUMES,
  tip="Great for following early potatoes in the same bed.",
  pot="a seed tray or deep modules", seeds="a pinch", depth="1 cm", germ="10 to 18 days", clump=True,
  space=15, row=30, layout="Rows 30 cm apart with a leek every 15 cm.",
  plant_depth="Make holes 15 cm deep with a dibber or the handle of a trowel.",
  plant_extra=["Trim the roots to about 2 cm and drop one leek into each hole.",
               "Fill each hole with water only, don't push soil back in. It settles naturally."],
  harvest=["Lift from autumn right through winter as you need them."])

P("scallion", "Scallions (spring onions)", "🌱", "VEG", "ALLIUM", 60, 8, directWeeksAfter=-3, frostHardy=True,
  feed="LOW", companions=["carrot", "lettuce", "beetroot"], avoid=LEGUMES, tip="Sow a short row every few weeks.",
  depth="1 cm", space=2, row=15, thin=False, germ="10 to 14 days",
  harvest=["Pull when they're about pencil thick."])

P("pea", "Pea", "🫛", "VEG", "LEGUME", 75, 6, directWeeksAfter=-4, frostHardy=True, feed="LOW",
  companions=["carrot", "radish", "lettuce", "earlypotato", "sweetcorn", "spinach"], avoid=ALLIUMS,
  tip="Peas fix their own nitrogen so they leave the soil richer.",
  prep=SOAK("pea", ["Soaking gives faster, more even germination."]),
  depth="4 cm", space=7, row=60, thin=False, germ="7 to 14 days",
  sow_extra=["Make a flat-bottomed trench 20 cm wide and sow three staggered rows of peas 7 cm apart.",
             "Put up pea sticks or netting along the row at sowing time."],
  layout="A 20 cm wide band of peas with netting up the middle, then 60 cm before the next band.",
  care=["Mice and birds love peas. Cover with netting or holly until they come up."],
  harvest=["Pick pods when they're plump but before the peas go hard.",
           "Pick every couple of days to keep them coming."])

P("mangetout", "Mangetout and sugar snap", "🫛", "VEG", "LEGUME", 65, 6, directWeeksAfter=-3, frostHardy=True,
  feed="LOW", companions=["carrot", "radish", "lettuce"], avoid=ALLIUMS, tip="Eat pods and all.",
  prep=SOAK("mangetout"), depth="4 cm", space=7, row=60, thin=False, germ="7 to 14 days",
  sow_extra=["Sow in a double row either side of netting or pea sticks."],
  layout="A double row against a net, 60 cm before the next row.",
  harvest=["Pick mangetout while flat, before the peas swell.", "Pick sugar snaps when the pods are round and crisp."])

P("broadbean", "Broad bean", "🫘", "VEG", "LEGUME", 100, 4, directWeeksAfter=-8, frostHardy=True, tunnel="OUTDOOR",
  feed="LOW", companions=["earlypotato", "maincrop", "sweetcorn", "spinach"], avoid=ALLIUMS,
  tip="Aquadulce can also be sown in November for an extra early crop.",
  station=True, seeds=1, depth="5 cm", space=20, row=60, thin=False, germ="10 to 14 days",
  layout="Double rows 20 cm apart with the plants staggered, then a 60 cm path before the next double row.",
  care=["Once the first pods form, pinch out the top 8 cm. This beats blackfly and the tips are lovely steamed.",
        "Put a cane at each corner of the double row and run string around to hold them up."],
  harvest=["Pick when you can see the beans bulging through the pod, before the scar on each bean turns black."])

P("runnerbean", "Runner bean", "🫘", "VEG", "LEGUME", 70, 10, directWeeksAfter=3, tunnel="OUTDOOR", water="HIGH",
  companions=["sweetcorn", "pumpkin", "courgette", "nasturtium"], avoid=ALLIUMS + ["beetroot"],
  tip="Keep picking to keep them coming.",
  station=True, seeds=2, depth="5 cm", space=30, row=60, germ="7 to 14 days",
  sow_extra=["Put the canes up first, then sow two seeds at the base of each cane."],
  layout="Two rows of 2.4 m canes 60 cm apart, crossed and tied at the top, or a wigwam of six to eight canes. One plant per cane.",
  care=["Wind young shoots anticlockwise round the canes to get them started.",
        "Water well once flowering starts and mist the flowers in dry spells to help them set."],
  harvest=["Pick at about 20 cm before the beans inside start to bulge.",
           "Never let pods ripen on the plant or it stops flowering."])

P("frenchbean", "French bean", "🫘", "VEG", "LEGUME", 60, 8, directWeeksAfter=3, feed="LOW",
  companions=["sweetcorn", "cucumber", "earlypotato", "strawberry"], avoid=ALLIUMS,
  tip="Dwarf types are perfect for small beds and pots.",
  station=True, seeds=2, depth="5 cm", space=15, row=45, germ="7 to 14 days",
  layout="Dwarf types in a block 15 cm apart. Climbing types up canes like runner beans.",
  harvest=["Pick when pencil thick and they snap cleanly."])

P("asparagus", "Asparagus", "🌱", "VEG", "OTHER", 390, 8, directWeeksAfter=-6, frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, feed="HIGH", tip="A bed lasts 20 years so give it a permanent spot.",
  depth="crowns 10 cm deep", space=30, row=45,
  planting=["Dig a trench 30 cm wide and 20 cm deep and make a ridge of soil down the middle.",
            "Sit each crown on top of the ridge and spread the roots out like a spider.",
            "Space them 30 cm apart and cover with 5 cm of soil.",
            "Fill the trench up gradually as the shoots grow through."],
  layout="One or two rows down the middle of its own bed.",
  care=["Let all the ferny growth grow in the first two years. Cut it down when it yellows in autumn.",
        "Mulch with compost every spring."],
  harvest=["Don't cut anything in the first two years.",
           "From year three, cut spears at 15 cm from April until mid June, then let them grow."])

P("artichoke", "Globe artichoke", "🌿", "VEG", "DAISY", 120, 8, directWeeksAfter=0, frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, tip="A handsome perennial that crops for years.",
  depth="same as in its pot", space=90, row=90,
  planting=["Plant young plants at the same depth they were growing in the pot.", "Give each plant 90 cm all round."],
  care=["Mulch the crowns with straw in winter in cold spots."],
  harvest=["Cut the heads while the scales are still tight, before they start to open."])

P("jerusalem", "Jerusalem artichoke", "🥔", "VEG", "DAISY", 200, 16, directWeeksAfter=-6, frostHardy=True,
  tunnel="OUTDOOR", planted=True, solo=True, water="LOW", feed="LOW",
  tip="Spreads easily. Every tuber left behind grows again.",
  depth="10 to 15 cm", space=30, row=90,
  planting=["Plant tubers 10 to 15 cm deep and 30 cm apart."],
  layout="One row along the back of a bed. Plants reach two metres and make a good windbreak.",
  care=["Earth up the stems and cut them back to 1.5 m in late summer so they don't blow over."],
  harvest=["Dig as needed from November.", "Lift every last one if you don't want them back next year."])

# =====================================================================
# FRUIT
# =====================================================================
P("strawberry", "Strawberry", "🍓", "FRUIT", "OTHER", 60, 6, directWeeksAfter=-3, frostHardy=True, tunnel="LIKES",
  perennial=True, planted=True, companions=["lettuce", "spinach", "onion", "garlic", "borage", "chives", "frenchbean"],
  avoid=BRASSICAS, tip="Replace plants every three years for the best crops.",
  depth="crown at soil level", space=35, row=75,
  planting=["Soak the roots of bare-root plants in a bucket for an hour first.",
            "Plant with the crown (where the leaves meet the roots) exactly at soil level. Too deep and it rots, too high and it dries out.",
            "Space them 35 cm apart in rows 75 cm apart."],
  layout="Rows or a grid 35 cm apart. Covering the bed with black weed fabric and planting through slits keeps weeds down and fruit clean.",
  care=["Tuck straw under the plants as the fruit forms.", "Net against birds once the berries start to colour.",
        "Cut off runners unless you want new plants."],
  harvest=["Pick when fully red, ideally on a warm afternoon for the best flavour."])

P("raspberry", "Summer raspberry", "🍇", "FRUIT", "OTHER", 240, 5, fixedSow=[11, 15], frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, solo=True,
  tip="After fruiting, cut the old canes to the ground and tie in the new ones.",
  depth="roots 5 to 8 cm deep", space=45, row=180,
  planting=["Put a strong post at each end of the row with wires at 60 cm, 1 m and 1.5 m.",
            "Plant canes 45 cm apart with the roots 5 to 8 cm below the surface.",
            "Cut each cane down to 25 cm after planting. It feels harsh but gives strong new canes."],
  layout="A single row along a post and wire fence.",
  care=["Mulch every spring with compost.", "Tie in new canes to the wires as they grow."],
  harvest=["Pick when the berries pull off the plug easily."])

P("autumnrasp", "Autumn raspberry", "🍇", "FRUIT", "OTHER", 290, 8, fixedSow=[11, 15], frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, solo=True,
  tip="The easiest raspberry. Cut every cane to the ground in February.",
  depth="roots 5 to 8 cm deep", space=45, row=150,
  planting=["Plant canes 45 cm apart with roots 5 to 8 cm deep.", "Cut each cane down to 25 cm after planting."],
  layout="A row or block. They're shorter so a couple of strings either side is enough support.",
  care=["Cut all canes to the ground every February.", "Mulch with compost in spring."],
  harvest=["Pick from late August until the first frosts."])

P("blackcurrant", "Blackcurrant", "⚫", "FRUIT", "OTHER", 240, 3, fixedSow=[11, 20], frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, solo=True, feed="HIGH", tip="Packed with vitamin C.",
  depth="5 cm deeper than in its pot", space=150, row=150,
  planting=["Plant 5 cm deeper than it was growing. This encourages new shoots from the base.",
            "Space bushes 1.5 m apart.", "Cut all shoots back to two buds after planting."],
  care=["Each winter cut out a third of the oldest, darkest stems at the base."],
  harvest=["Pick whole strings of berries when they're all black and shiny."])

P("redcurrant", "Redcurrant", "🔴", "FRUIT", "OTHER", 240, 3, fixedSow=[11, 20], frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, solo=True, tip="Easy and happy in a bit of shade.",
  depth="same as in its pot", space=120, row=150,
  planting=["Plant at the same depth it was growing.", "Space bushes 1.2 m apart."],
  care=["Net against birds when the fruit colours.", "Prune in winter to an open goblet shape."],
  harvest=["Pick whole strings when the berries are bright red and glossy."])

P("gooseberry", "Gooseberry", "🟢", "FRUIT", "OTHER", 230, 4, fixedSow=[11, 20], frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, solo=True, tip="Wear gloves. They're thorny.",
  depth="same as in its pot", space=120, row=150,
  planting=["Plant at the same depth it was growing.", "Space bushes 1.2 m apart."],
  care=["Prune in winter to an open goblet shape so air flows through and you can reach the fruit.",
        "Check for sawfly caterpillars in spring. They can strip a bush in days."],
  harvest=["Pick some green in early summer for cooking and leave the rest to ripen sweet."])

P("blueberry", "Blueberry", "🫐", "FRUIT", "OTHER", 120, 6, directWeeksAfter=-4, frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, feed="LOW",
  tip="Needs acid soil. A big pot of ericaceous compost is easiest.",
  depth="same as in its pot", space=120, row=120,
  planting=["Plant into ericaceous (acid) compost, either in a large pot or a bed dug out and filled with it.",
            "Plant at the same depth it was growing.", "Grow two varieties for better pollination."],
  care=["Water with rainwater, not tap water. Tap water slowly makes the soil less acid.",
        "Net against birds once the berries turn blue."],
  harvest=["Pick when the berries are fully blue and come away easily. Wait a few days after they first colour."])

P("rhubarb", "Rhubarb", "🟥", "FRUIT", "OTHER", 60, 10, directWeeksAfter=-8, frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, feed="HIGH",
  tip="Leave it alone in its first year so the crown builds strength.",
  depth="buds at the surface", space=90, row=90,
  planting=["Dig a big hole and mix in plenty of compost or well-rotted manure.",
            "Plant the crown with the buds just at the surface.", "Allow 90 cm all round each plant."],
  care=["Mulch around (not on top of) the crown every spring.", "Cut off any flower stalks as they appear."],
  harvest=["Pull stalks from the second year by holding at the base and twisting. Never take more than half the stalks.",
           "Stop picking by July so the plant can recover."])

P("apple", "Apple tree", "🍎", "FRUIT", "OTHER", 280, 6, fixedSow=[12, 1], frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, tip="Pick a variety on a dwarfing rootstock for small gardens.",
  depth="graft union 5 cm above soil", space=300, row=300,
  planting=["Soak bare roots in a bucket of water for an hour.",
            "Dig a hole wider than the roots but no deeper.",
            "Drive a stake into the hole on the windward side before planting.",
            "Set the tree so the graft union (the bump low on the stem) is 5 cm above the soil.",
            "Backfill, firm with your boot and tie the tree to the stake."],
  layout="A single tree in its own spot with a 1 m circle of bare soil or mulch around the trunk.",
  care=["Keep a circle of grass and weeds clear round the trunk for the first few years.",
        "Prune in winter to keep the middle open."],
  harvest=["Ready when an apple comes away with a gentle lift and twist.",
           "A young tree may take two or three years to give a real crop."])

P("pear", "Pear tree", "🍐", "FRUIT", "OTHER", 290, 4, fixedSow=[12, 1], frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, tip="Likes a sheltered sunny spot. Conference is reliable in Ireland.",
  depth="graft union 5 cm above soil", space=300, row=300,
  planting=["Soak bare roots for an hour.", "Drive a stake in first, then plant at the same depth as before.",
            "Keep the graft union above the soil, firm in well and tie to the stake."],
  care=["Keep the base weed free and mulch in spring."],
  harvest=["Pick pears while still firm and ripen them indoors for a week or two."])

P("plum", "Plum tree", "🟣", "FRUIT", "OTHER", 260, 3, fixedSow=[12, 1], frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, tip="Victoria is self fertile and a great all rounder.",
  depth="graft union 5 cm above soil", space=300, row=300,
  planting=["Soak bare roots for an hour.", "Drive a stake in first, then plant at the same depth as before.",
            "Firm in well and tie to the stake."],
  care=["Only prune in summer. Winter pruning lets in silver leaf disease."],
  harvest=["Pick when they're soft and come away easily."])

P("melon", "Melon", "🍈", "FRUIT", "CUCURBIT", 85, 4, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=4, tunnel="NEEDS",
  water="HIGH", feed="HIGH", companions=["nasturtium"], avoid=["earlypotato", "maincrop"],
  tip="A real treat from a tunnel in a good summer.",
  pot="9 cm pots", seeds=1, depth="1.5 cm", germ="5 to 10 days at 24°C",
  sow_extra=["Sow on its edge so it doesn't rot."], warmth="Keep them really warm, around 24°C.",
  space=90, row=90, layout="One row along the warmest side of the tunnel, trained up strings or netting.",
  plant_depth="Plant with the top of the rootball slightly proud of the soil.",
  care=["Pinch out the main tip after five leaves and train side shoots up the support.",
        "If fruit isn't setting, dab pollen from male flowers into female flowers with a small brush.",
        "Keep four fruits per plant and support each one in a net or old pair of tights."],
  harvest=["Ripe when it smells sweet and the stalk starts to crack around the top."])

P("capegooseberry", "Cape gooseberry", "🟠", "FRUIT", "SOLANACEAE", 110, 8, sowIndoorsWeeksBefore=8,
  plantOutWeeksAfter=3, tunnel="NEEDS", tip="Fruit comes wrapped in little paper lanterns.",
  pot="modules", seeds=2, depth="0.5 cm", germ="10 to 14 days at 20°C", pot_on=True,
  space=60, row=60, plant_extra=["Put a cane beside each plant."],
  harvest=["Pick when the lanterns turn papery and the fruit inside is golden."])

# =====================================================================
# HERBS
# =====================================================================
P("basil", "Basil", "🌿", "HERB", "OTHER", 40, 14, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=4, tunnel="NEEDS",
  feed="LOW", companions=["tomato", "pepper", "chilli", "aubergine"], tip="Water in the morning and never let it get cold.",
  pot="modules", seeds=4, depth="surface", germ="5 to 10 days at 20°C", clump=True,
  space=25, row=25, layout="Dot plants between tomatoes in the tunnel or grow in pots on a sunny windowsill.",
  care=["Pinch out the tips to make it bushy and stop it flowering."],
  harvest=["Pick from the top, just above a pair of leaves, and it will branch out."])

P("parsley", "Parsley", "🌿", "HERB", "UMBELLIFER", 70, 24, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=0,
  frostHardy=True, feed="LOW", companions=["tomato", "chives"], tip="Flat leaf has more flavour, curly is hardier.",
  prep=SOAK("parsley", ["Use warm water. It helps with famously slow germination."]),
  pot="modules", seeds=4, depth="0.5 cm", germ="3 to 4 weeks", clump=True,
  space=20, row=25,
  harvest=["Pick outer stalks from the base and it keeps going for months."])

P("coriander", "Coriander", "🌿", "HERB", "UMBELLIFER", 40, 4, directWeeksAfter=1, feed="LOW",
  companions=["spinach", "lettuce"], tip="Sow direct as it hates being moved. Sow every three weeks.",
  depth="1 cm", space=5, row=20, germ="7 to 14 days",
  harvest=["Cut leaves when plants are 10 cm tall.", "Let a few flower and collect the seeds for cooking."])

P("chives", "Chives", "🌿", "HERB", "ALLIUM", 60, 24, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=0, frostHardy=True,
  perennial=True, water="LOW", feed="LOW", companions=["carrot", "tomato", "strawberry", "lettuce"], avoid=LEGUMES,
  tip="The purple flowers are edible and the bees love them.",
  pot="modules", seeds="8 to 10", depth="0.5 cm", germ="10 to 14 days", clump=True,
  space=25, row=25, layout="Small clumps along the edge of a bed.",
  harvest=["Snip leaves 3 cm above the base. They grow back fast."])

P("dill", "Dill", "🌿", "HERB", "UMBELLIFER", 50, 6, directWeeksAfter=2, feed="LOW",
  companions=["cucumber", "lettuce"] + BRASSICAS, avoid=["carrot"], tip="Brings in hoverflies that eat aphids.",
  depth="0.5 cm", space=20, row=30, germ="7 to 21 days",
  harvest=["Snip the feathery leaves as needed."])

P("mint", "Mint", "🌿", "HERB", "OTHER", 40, 20, directWeeksAfter=0, frostHardy=True, perennial=True, planted=True,
  solo=True, tunnel="OUTDOOR", feed="LOW", tip="Grow it in a pot or it will take over the whole bed.",
  depth="same as in its pot", space=30, row=30,
  planting=["Plant into a large pot, or sink a bottomless bucket into the bed and plant into that to stop it spreading."],
  care=["Cut it back hard in midsummer for fresh new leaves."],
  harvest=["Pick sprigs whenever you need them."])

P("thyme", "Thyme", "🌿", "HERB", "OTHER", 90, 24, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=2, frostHardy=True,
  perennial=True, water="LOW", feed="LOW", companions=["cabbage", "strawberry"],
  tip="Likes poor dry soil and full sun.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="14 to 28 days", clump=True,
  sow_extra=["Prick out seedlings into modules when they're big enough to handle."],
  space=30, row=30, layout="Along a sunny edge of a bed or between paving stones.",
  harvest=["Snip sprigs as needed. Trim lightly after flowering."])

P("rosemary", "Rosemary", "🌿", "HERB", "OTHER", 90, 40, directWeeksAfter=2, frostHardy=True, perennial=True,
  planted=True, water="LOW", feed="LOW", tunnel="OUTDOOR", tip="Easiest to buy as a plant.",
  depth="same as in its pot", space=60, row=60,
  planting=["Plant in the sunniest, best-drained spot you have. It hates wet feet.",
            "Plant at the same depth it was growing."],
  harvest=["Snip sprigs all year round."])

P("sage", "Sage", "🌿", "HERB", "OTHER", 90, 30, directWeeksAfter=2, frostHardy=True, perennial=True, planted=True,
  water="LOW", feed="LOW", tunnel="OUTDOOR", avoid=["cucumber"], tip="Replace every four or five years when it gets woody.",
  depth="same as in its pot", space=45, row=45,
  planting=["Plant in a sunny well-drained spot at the same depth it was growing."],
  harvest=["Pick leaves as needed and trim after flowering."])

P("oregano", "Oregano", "🌿", "HERB", "OTHER", 80, 20, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=2,
  frostHardy=True, perennial=True, water="LOW", feed="LOW", tip="Bees love the flowers.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="7 to 14 days", clump=True,
  sow_extra=["Prick out into modules when big enough to handle."],
  space=30, row=30,
  harvest=["Pick sprigs as needed and cut back after flowering."])

# =====================================================================
# FLOWERS
# =====================================================================
P("marigold", "French marigold", "🌼", "FLOWER", "DAISY", 60, 16, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=2,
  tunnel="LIKES", feed="LOW", companions=["tomato", "pepper", "chilli", "aubergine", "cucumber", "earlypotato"] + BRASSICAS,
  tip="The scent helps keep whitefly off tomatoes.",
  pot="modules", seeds=2, depth="0.5 cm", germ="5 to 7 days at 20°C",
  space=25, row=25, layout="Dot them between tomatoes in the tunnel and along the edges of veg beds.",
  care=["Snap off dead flowers to keep new ones coming."], harvest=["Enjoy the colour all summer long."])

P("nasturtium", "Nasturtium", "🌺", "FLOWER", "OTHER", 50, 16, directWeeksAfter=2, water="LOW", feed="LOW",
  companions=["courgette", "cucumber", "pumpkin", "squash", "earlypotato", "melon"] + BRASSICAS,
  tip="A sacrificial crop. Aphids and caterpillars go for it instead of your veg.",
  prep=SOAK("nasturtium", ["It softens their tough coat."]),
  station=True, seeds=2, depth="2 cm", space=30, row=30, germ="7 to 14 days",
  layout="Along the edges of beds so they trail over the sides.",
  care=["Don't feed them. Rich soil gives lots of leaves and few flowers."],
  harvest=["Leaves and flowers are edible with a peppery kick."])

P("calendula", "Calendula", "🌼", "FLOWER", "DAISY", 60, 16, directWeeksAfter=-2, frostHardy=True, feed="LOW",
  companions=["tomato", "earlypotato", "kale", "cabbage"], tip="Edible petals and a magnet for pollinators.",
  depth="1 cm", space=30, row=30, germ="7 to 14 days",
  care=["Deadhead to keep it flowering."], harvest=["Pick flowers for salads or vases."])

P("sunflower", "Sunflower", "🌻", "FLOWER", "DAISY", 90, 6, sowIndoorsWeeksBefore=3, plantOutWeeksAfter=3,
  tunnel="OUTDOOR", avoid=["earlypotato", "maincrop"], tip="A fun one for kids. Measure them every week.",
  pot="9 cm pots", seeds=2, depth="2 cm", germ="7 to 10 days",
  space=45, row=60, layout="At the back of a bed so they don't shade other crops.",
  plant_extra=["Push in a tall cane beside each one and tie it in as it grows."],
  care=["Protect young plants from slugs."],
  harvest=["Cut for the vase, or leave the heads for the birds in autumn."])

P("sweetpea", "Sweet pea", "💐", "FLOWER", "LEGUME", 100, 10, sowIndoorsWeeksBefore=14, plantOutWeeksAfter=-2,
  frostHardy=True, tunnel="OUTDOOR", tip="The more you pick, the more flowers you get.",
  prep=SOAK("sweet pea", ["Next day, any seeds that haven't swelled up need their coat nicked with nail clippers, away from the eye, and another soak."]),
  pot="root trainers or toilet roll tubes (they like a long root run)", seeds=2, depth="1.5 cm",
  germ="10 to 14 days at 15°C", clump=True,
  warmth="Keep them cool and bright. Too much heat makes them weak and leggy.",
  space=20, row=60, layout="A wigwam of canes or a row of netting with one plant at the base of each cane.",
  plant_extra=["If grown in toilet roll tubes, plant tube and all."],
  care=["Pinch out the tip when plants have three pairs of leaves for bushier plants.",
        "Tie stems to the canes as they grow."],
  harvest=["Pick every few days. If they set seed pods they stop flowering."])

P("cosmos", "Cosmos", "🌸", "FLOWER", "DAISY", 80, 14, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=2, feed="LOW",
  tip="Flowers right up to the first frosts.",
  pot="modules", seeds=2, depth="1 cm", germ="7 to 10 days",
  space=40, row=40, care=["Pinch out the tips at 30 cm for bushier plants.", "Deadhead regularly."],
  harvest=["Cut for vases as often as you like."])

P("zinnia", "Zinnia", "🌺", "FLOWER", "DAISY", 75, 10, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3, tunnel="LIKES",
  feed="LOW", tip="Loves heat. Grows brilliantly in the tunnel.",
  pot="modules", seeds=2, depth="0.5 cm", germ="5 to 7 days at 21°C",
  space=30, row=30, care=["Water at the base to avoid mildew."], harvest=["Cut flowers when fully open."])

P("cornflower", "Cornflower", "🔵", "FLOWER", "DAISY", 75, 10, directWeeksAfter=-3, frostHardy=True, feed="LOW",
  tip="Bees and butterflies love it.", depth="1 cm", space=25, row=30, germ="7 to 14 days",
  harvest=["Cut for vases when the flowers are just opening."])

P("nigella", "Love-in-a-mist", "🌸", "FLOWER", "OTHER", 80, 8, directWeeksAfter=-2, frostHardy=True, feed="LOW",
  tip="Self seeds happily and the seed pods look great dried.",
  depth="0.5 cm", space=20, row=25, germ="10 to 14 days",
  harvest=["Cut flowers or leave the seed pods to dry for arrangements."])

P("poppy", "Poppy", "🌺", "FLOWER", "OTHER", 90, 6, directWeeksAfter=-3, frostHardy=True, feed="LOW",
  tip="Sow where they're to flower. They hate being moved.",
  broadcast=True, depth="surface", space=25, row=25, germ="10 to 21 days",
  layout="A loose drift scattered across a sunny patch.",
  care=["Thin seedlings to about 25 cm apart."], harvest=["Enjoy in the garden. They don't last long in a vase."])

P("snapdragon", "Snapdragon", "🌸", "FLOWER", "OTHER", 90, 12, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=0,
  frostHardy=True, feed="LOW", tip="Kids love squeezing the dragon's mouth open.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="10 to 21 days", clump=True,
  sow_extra=["Prick out into modules when they have two true leaves."],
  space=25, row=30, care=["Pinch out the tips for bushier plants and deadhead often."],
  harvest=["Cut when the bottom flowers on the spike open."])

P("viola", "Viola and pansy", "🌸", "FLOWER", "OTHER", 90, 20, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=0,
  frostHardy=True, feed="LOW", tip="Sow in summer for winter and spring colour too.",
  pot="a seed tray", seeds="a pinch", depth="0.5 cm", germ="10 to 21 days in the dark", clump=True,
  sow_extra=["Cover the tray with newspaper. They germinate best in the dark.",
             "Prick out into modules once they have two true leaves."],
  space=20, row=20, care=["Deadhead regularly."], harvest=["The flowers are edible and pretty on cakes."])

P("echinacea", "Echinacea (coneflower)", "🌸", "FLOWER", "DAISY", 120, 10, sowIndoorsWeeksBefore=8,
  plantOutWeeksAfter=2, frostHardy=True, perennial=True, feed="LOW", tip="A long-lived perennial for pollinators.",
  prep=CHILL("echinacea", 4), pot="modules", seeds=2, depth="0.5 cm", germ="10 to 21 days at 20°C",
  space=45, row=45, harvest=["Leave the seed heads over winter for the birds."])

P("lupin", "Lupin", "🪻", "FLOWER", "LEGUME", 120, 6, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=2,
  frostHardy=True, perennial=True, feed="LOW", tip="Watch out for slugs. They adore lupins.",
  prep=SOAK("lupin", ["If any haven't swelled by morning, nick the seed coat with a nail file and soak again."]),
  pot="deep 9 cm pots", seeds=2, depth="1 cm", germ="10 to 20 days",
  space=45, row=45, plant_extra=["Plant out young. Lupins have a long tap root that hates being disturbed."],
  care=["Cut off faded spikes for a second flush of flowers."], harvest=["Enjoy the spires in early summer."])

P("foxglove", "Foxglove", "🔔", "FLOWER", "OTHER", 330, 6, fixedSow=[6, 1], frostHardy=True, feed="LOW",
  tip="A biennial. Sow this summer, flowers next summer. All parts are poisonous.",
  broadcast=True, depth="surface", space=45, row=45, germ="14 to 21 days",
  layout="Scattered in a shady corner or at the back of a border.",
  care=["Thin or move seedlings to 45 cm apart in autumn."], harvest=["Enjoy the spires in the garden."])

P("wallflower", "Wallflower", "🧡", "FLOWER", "BRASSICA", 300, 8, fixedSow=[6, 1], frostHardy=True, feed="LOW",
  tip="A biennial with a gorgeous scent. Sow in early summer for flowers next spring.",
  depth="1 cm", space=30, row=30, germ="7 to 14 days",
  care=["Move to where they'll flower in October."], harvest=["Cut a few for a scented vase in spring."])

P("borage", "Borage", "💙", "FLOWER", "OTHER", 60, 14, directWeeksAfter=0, water="LOW", feed="LOW",
  companions=["strawberry", "tomato", "courgette"], tip="Bees adore it and it self seeds happily.",
  station=True, seeds=2, depth="1 cm", space=40, row=40, germ="7 to 14 days",
  harvest=["The blue flowers are lovely in salads or frozen in ice cubes."])

P("phacelia", "Phacelia", "🐝", "FLOWER", "OTHER", 60, 6, directWeeksAfter=2, feed="LOW",
  tip="A green manure and bee magnet. Great for resting a bed.", broadcast=True, depth="1 cm", space=10, row=10,
  germ="7 to 10 days", layout="Scattered over an empty bed.",
  harvest=["Dig it in before it sets seed, or leave the flowers for the bees and dig in afterwards."])

P("dahlia", "Dahlia", "🏵️", "FLOWER", "DAISY", 90, 12, directWeeksAfter=2, tunnel="OUTDOOR", planted=True,
  feed="HIGH", tip="Plant tubers after the frosts and lift or mulch them in autumn.",
  depth="15 cm", space=60, row=60,
  planting=["Dig a hole 15 cm deep and lay the tuber in with the old stem pointing up.",
            "Push in a stake beside it now so you don't spear the tuber later.", "Space them 60 cm apart."],
  care=["Protect new shoots from slugs.", "Pinch out the tip at 30 cm for more flowers."],
  harvest=["Cut flowers often and you'll get more.",
           "After the first frost blackens the leaves, lift the tubers to store, or mulch thickly in mild areas."])

P("gladiolus", "Gladiolus", "🌺", "FLOWER", "OTHER", 100, 4, directWeeksAfter=0, tunnel="OUTDOOR", planted=True,
  feed="LOW", tip="Plant a few every two weeks for a longer show.", depth="10 cm", space=15, row=30,
  planting=["Plant corms 10 cm deep with the pointed end up.", "Space them 15 cm apart."],
  care=["Stake tall spikes before they flower."], harvest=["Cut when the lowest flower on the spike opens."])

P("tulip", "Tulip bulbs", "🌷", "FLOWER", "OTHER", 160, 4, fixedSow=[11, 10], frostHardy=True, tunnel="OUTDOOR",
  planted=True, water="LOW", feed="LOW", tip="Plant in November. Later planting avoids tulip fire disease.",
  depth="15 cm", space=10, row=10,
  planting=["Plant bulbs pointy end up, about 15 cm deep. That's three times the height of the bulb.",
            "Space them 10 cm apart in groups for the best show."],
  layout="Clumps or drifts rather than single lines.",
  harvest=["Cut for vases when the buds are coloured but still closed.",
           "Let the leaves die back naturally after flowering."])

P("daffodil", "Daffodil bulbs", "🌼", "FLOWER", "OTHER", 180, 4, fixedSow=[9, 25], frostHardy=True, tunnel="OUTDOOR",
  planted=True, perennial=True, water="LOW", feed="LOW", tip="Plant by the end of September. They come back every year.",
  depth="12 to 15 cm", space=10, row=10,
  planting=["Plant bulbs pointy end up at about three times their own depth.", "Space 10 cm apart in clumps."],
  layout="Natural-looking clumps under trees, in grass or along edges.",
  harvest=["Leave the leaves for six weeks after flowering so the bulb builds up for next year."])

P("lavender", "Lavender", "💜", "FLOWER", "OTHER", 90, 8, directWeeksAfter=2, frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, water="LOW", feed="LOW", tip="Trim after flowering but never cut into old wood.",
  depth="same as in its pot", space=45, row=45,
  planting=["Choose the sunniest, best-drained spot you have. Add grit to heavy soil.",
            "Plant at the same depth it was growing."],
  layout="A low hedge along a sunny path or bed edge.",
  harvest=["Cut stems when the first flowers open for drying."])



# =====================================================================
# MORE HERBS
# =====================================================================
P("lemonbalm", "Lemon balm", "🌿", "HERB", "OTHER", 60, 20, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=2,
  frostHardy=True, perennial=True, feed="LOW", tip="Lovely as a tea. It spreads, so a pot keeps it in check.",
  pot="modules", seeds="a pinch", depth="surface", germ="10 to 14 days", clump=True,
  space=40, row=40, harvest=["Pick leaves any time. Cut back after flowering for fresh growth."])

P("marjoram", "Sweet marjoram", "🌿", "HERB", "OTHER", 70, 16, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=2,
  water="LOW", feed="LOW", tip="Milder and sweeter than oregano.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="7 to 14 days", clump=True,
  sow_extra=["Prick out into modules once big enough to handle."],
  space=25, row=25, harvest=["Pick sprigs as needed. Best just before it flowers."])

P("chervil", "Chervil", "🌿", "HERB", "UMBELLIFER", 45, 8, directWeeksAfter=-2, frostHardy=True, feed="LOW",
  tip="One of the few herbs that likes a bit of shade.", depth="1 cm", space=15, row=25, germ="10 to 14 days",
  harvest=["Snip leaves from the outside. Sow again every month for a steady supply."])

P("sorrel", "Sorrel", "🍃", "HERB", "OTHER", 60, 30, directWeeksAfter=-2, frostHardy=True, perennial=True,
  feed="LOW", tip="Sharp lemony leaves. Comes back year after year.", depth="1 cm", space=30, row=30, germ="7 to 14 days",
  care=["Cut off flower stems so it keeps making leaves."], harvest=["Pick young leaves from spring to autumn."])

P("garlicchives", "Garlic chives", "🌿", "HERB", "ALLIUM", 70, 24, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=0,
  frostHardy=True, perennial=True, water="LOW", feed="LOW", avoid=LEGUMES, tip="Flat leaves with a garlicky taste.",
  pot="modules", seeds="8 to 10", depth="0.5 cm", germ="10 to 14 days", clump=True,
  space=25, row=25, harvest=["Snip leaves 3 cm above the base and they regrow."])

P("chamomile", "Chamomile", "🌼", "HERB", "DAISY", 70, 10, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=1,
  frostHardy=True, water="LOW", feed="LOW", tip="German chamomile is the one for tea.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="7 to 14 days", clump=True,
  sow_extra=["Don't cover the seed. It needs light to germinate."],
  space=20, row=25, harvest=["Pick the flower heads when fully open and dry them for tea."])

P("tarragon", "French tarragon", "🌿", "HERB", "DAISY", 60, 20, directWeeksAfter=2, perennial=True, planted=True,
  water="LOW", feed="LOW", tunnel="OUTDOOR", tip="Buy a plant. French tarragon doesn't come true from seed.",
  depth="same as in its pot", space=45, row=45,
  planting=["Plant in a sunny well-drained spot at the same depth it was growing."],
  care=["Mulch the roots in winter in cold gardens."], harvest=["Pick sprigs from early summer."])

P("bay", "Bay tree", "🌿", "HERB", "OTHER", 90, 52, directWeeksAfter=2, frostHardy=True, perennial=True,
  planted=True, solo=True, water="LOW", feed="LOW", tunnel="OUTDOOR", potSize="40 cm pot or bigger",
  tip="Perfect in a pot by the back door. Clip it to shape in summer.",
  depth="same as in its pot", space=100, row=100,
  planting=["Plant in a sheltered sunny spot or a big pot of John Innes No 3 compost."],
  harvest=["Pick leaves all year round and dry some for the kitchen."])

P("lovage", "Lovage", "🌿", "HERB", "UMBELLIFER", 90, 20, directWeeksAfter=0, frostHardy=True, perennial=True,
  planted=True, solo=True, tunnel="OUTDOOR", tip="Tastes like strong celery. One plant is plenty, it grows tall.",
  depth="same as in its pot", space=60, row=60, planting=["Plant at the same depth it was growing in its pot."],
  harvest=["Pick young leaves for soups and stews."])

P("lemonverbena", "Lemon verbena", "🍋", "HERB", "OTHER", 90, 20, directWeeksAfter=3, perennial=True, planted=True,
  tunnel="LIKES", feed="LOW", potSize="30 cm pot", tip="Not fully hardy. Keep it in a pot and bring it in for winter.",
  depth="same as in its pot", space=60, row=60, planting=["Plant in a sunny sheltered spot or a pot."],
  harvest=["Pick leaves for tea right through summer."])

P("wildgarlic", "Wild garlic", "🌿", "HERB", "ALLIUM", 150, 8, fixedSow=[10, 1], frostHardy=True, perennial=True,
  planted=True, solo=True, tunnel="OUTDOOR", tip="Loves damp shade under trees. Spreads happily.",
  depth="5 cm", space=15, row=15, planting=["Plant bulbs or young plants 5 cm deep in a shady damp spot."],
  harvest=["Pick leaves from March to May. The white flowers are edible too."])

P("horseradish", "Horseradish", "🌿", "HERB", "BRASSICA", 200, 20, directWeeksAfter=-4, frostHardy=True,
  perennial=True, planted=True, solo=True, tunnel="OUTDOOR", tip="Very invasive. Grow it in a deep pot or old bin.",
  depth="5 cm", space=45, row=45, planting=["Plant root cuttings at an angle with the top 5 cm below the soil."],
  harvest=["Dig roots in autumn and grate fresh. Wear goggles, it's strong!"])

P("catmint", "Catmint (nepeta)", "💜", "FLOWER", "OTHER", 90, 16, directWeeksAfter=2, frostHardy=True,
  perennial=True, planted=True, water="LOW", feed="LOW", tip="One of the very best plants for bees.",
  depth="same as in its pot", space=45, row=45, planting=["Plant in a sunny spot at the same depth as in its pot."],
  care=["Cut back hard after the first flush for a second round of flowers."], harvest=["Enjoy the flowers."])

# =====================================================================
# MORE VEG
# =====================================================================
P("cherrytomato", "Cherry tomato", "🍅", "VEG", "SOLANACEAE", 65, 14, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=2,
  tunnel="LIKES", water="HIGH", feed="HIGH", companions=["basil", "marigold", "chives"],
  avoid=["earlypotato", "maincrop"] + BRASSICAS, potSize="30 cm pot or hanging basket for trailing types",
  tip="Easier than big tomatoes. Bush and trailing types grow well in pots outdoors.",
  pot="small pots or modules", seeds=2, depth="0.5 cm", germ="7 to 14 days at 18 to 24°C", pot_on=True,
  space=45, row=60, plant_depth="Plant deeper than they were in the pot. They root along the buried stem.",
  plant_extra=["Cordon types need a cane. Bush and trailing types don't."],
  care=["Pinch side shoots on cordon types only.", "Feed weekly with tomato feed once fruit sets."],
  harvest=["Pick when fully coloured. Pick little and often."])

P("springcabbage", "Spring cabbage", "🥬", "VEG", "BRASSICA", 200, 6, fixedSow=[8, 1], frostHardy=True,
  tunnel="OUTDOOR", feed="MEDIUM", avoid=["strawberry", "tomato"], tip="Sown in late summer for the first greens of spring.",
  depth="1 cm", space=30, row=30, germ="5 to 10 days",
  sow_extra=["Transplant seedlings to their final spot in September when they have five or six leaves."],
  care=["Firm in after winter winds and net against pigeons."],
  harvest=["Cut young loose heads as spring greens from March, or leave some to heart up."])

P("savoy", "Savoy cabbage", "🥬", "VEG", "BRASSICA", 120, 12, sowIndoorsWeeksBefore=0, plantOutWeeksAfter=5,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", avoid=["strawberry", "tomato"], tip="Crinkly leaves and very hardy.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days", space=50, row=50,
  plant_depth="Plant firmly up to the first leaves.", plant_extra=["Fit a collar around each stem against cabbage root fly."],
  harvest=["Cut through winter as you need them."])

P("redcabbage", "Red cabbage", "🟣", "VEG", "BRASSICA", 110, 8, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=2,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", avoid=["strawberry", "tomato"], tip="Stores for months in a cool shed.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days", space=45, row=45,
  plant_depth="Plant firmly up to the first leaves.", plant_extra=["Fit a collar around each stem against cabbage root fly."],
  harvest=["Cut when the head is firm and glossy."])

P("cavolonero", "Cavolo nero", "🥬", "VEG", "BRASSICA", 75, 24, sowIndoorsWeeksBefore=0, plantOutWeeksAfter=5,
  frostHardy=True, tunnel="OUTDOOR", feed="HIGH", avoid=["strawberry", "tomato"],
  tip="Tuscan black kale. Tall, handsome and delicious.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days", space=45, row=60,
  plant_depth="Plant firmly up to the first leaves.", care=["Net against butterflies and pigeons."],
  harvest=["Pick lower leaves and let the plant keep growing upwards."])

P("romanesco", "Romanesco", "🥦", "VEG", "BRASSICA", 100, 3, sowIndoorsWeeksBefore=2, plantOutWeeksAfter=3,
  frostHardy=True, tunnel="OUTDOOR", water="HIGH", feed="HIGH", avoid=["strawberry", "tomato"],
  tip="The spiral one. Treat it like cauliflower.",
  pot="modules", seeds=2, depth="1 cm", germ="5 to 10 days", space=60, row=60,
  plant_depth="Plant firmly up to the first leaves.", harvest=["Cut while the spirals are tight."])

P("endive", "Endive", "🥗", "VEG", "DAISY", 80, 8, directWeeksAfter=6, frostHardy=True, tunnel="LIKES", feed="LOW",
  tip="Slightly bitter salad leaf for late summer and autumn.", depth="1 cm", space=30, row=30, germ="7 to 14 days",
  care=["Cover the middle with a plate for a week before cutting to make it paler and less bitter."],
  harvest=["Cut whole heads or pick outer leaves."])

P("radicchio", "Radicchio", "🔴", "VEG", "DAISY", 90, 8, directWeeksAfter=8, frostHardy=True, feed="LOW",
  tip="The colour deepens as the weather turns cold.", depth="1 cm", space=30, row=30, germ="7 to 14 days",
  harvest=["Cut the tight heads in autumn and winter."])

P("landcress", "Land cress", "🌱", "VEG", "BRASSICA", 50, 16, directWeeksAfter=12, frostHardy=True, tunnel="LIKES",
  feed="LOW", tip="Peppery like watercress but grows in ordinary soil.", depth="0.5 cm", space=15, row=20,
  harvest=["Pick leaves through autumn and winter."])

P("mooli", "Mooli (winter radish)", "⚪", "VEG", "BRASSICA", 60, 8, directWeeksAfter=10, frostHardy=True, feed="LOW",
  tip="Sow in mid summer. Earlier sowings bolt.", depth="1.5 cm", space=15, row=30, germ="5 to 7 days",
  harvest=["Lift from autumn. They're fine in the ground until hard frost."])

P("salsify", "Salsify", "🥕", "VEG", "DAISY", 150, 12, directWeeksAfter=-2, frostHardy=True, tunnel="OUTDOOR",
  water="LOW", feed="LOW", tip="An old-fashioned root that tastes faintly of oysters.",
  depth="1 cm", space=10, row=30, germ="14 to 21 days", harvest=["Lift from October as needed."])

P("marrow", "Marrow", "🥒", "VEG", "CUCURBIT", 70, 6, sowIndoorsWeeksBefore=4, plantOutWeeksAfter=3,
  tunnel="OUTDOOR", water="HIGH", feed="HIGH", avoid=["earlypotato", "maincrop"],
  tip="Basically a courgette left to grow big.", pot="9 cm pots", seeds=1, depth="2 cm", germ="5 to 10 days",
  sow_extra=["Sow the seed on its edge so it doesn't rot."], space=100, row=100,
  plant_depth="Plant into a hollow so water collects round the roots.",
  harvest=["Cut when the skin is firm. Store somewhere cool for winter."])

P("cucamelon", "Cucamelon", "🍈", "VEG", "CUCURBIT", 75, 10, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=3,
  tunnel="NEEDS", water="HIGH", feed="MEDIUM", tip="Grape-sized fruit that taste of cucumber and lime. Kids love them.",
  pot="9 cm pots", seeds=2, depth="1 cm", germ="7 to 14 days at 21°C", space=45, row=60,
  plant_extra=["Give them strings or netting to climb."], harvest=["Pick when grape sized and firm."])

P("tomatillo", "Tomatillo", "🟢", "VEG", "SOLANACEAE", 80, 8, sowIndoorsWeeksBefore=8, plantOutWeeksAfter=3,
  tunnel="NEEDS", feed="MEDIUM", tip="Grow two plants, they need a partner to pollinate.",
  pot="modules", seeds=2, depth="0.5 cm", germ="7 to 14 days at 21°C", pot_on=True, space=60, row=60,
  plant_extra=["Stake each plant."], harvest=["Pick when the fruit fills and splits its papery husk."])

P("sweetpotato", "Sweet potato", "🍠", "VEG", "OTHER", 120, 2, directWeeksAfter=4, tunnel="NEEDS", planted=True,
  water="MEDIUM", feed="MEDIUM", potSize="40 litre tub", tip="Needs the tunnel in Ireland. Buy rooted slips in May.",
  depth="up to the top leaves", space=30, row=75,
  planting=["Plant slips deep so only the top leaves show.", "Space them 30 cm apart along a ridge of soil."],
  harvest=["Lift carefully in October before the first frost.", "Leave somewhere warm for a week to cure."])

P("borlotti", "Borlotti beans", "🫘", "VEG", "LEGUME", 100, 4, directWeeksAfter=3, tunnel="OUTDOOR",
  companions=["sweetcorn", "squash"], avoid=ALLIUMS, tip="Grown for the speckled dried beans.",
  station=True, seeds=2, depth="5 cm", space=30, row=60, germ="7 to 14 days",
  sow_extra=["Put the canes up first and sow two seeds at the base of each."],
  layout="A double row of canes or a wigwam, like runner beans.",
  harvest=["Leave pods on the plant until they're dry and papery, then pod and store the beans."])

P("nzspinach", "New Zealand spinach", "🍃", "VEG", "OTHER", 60, 16, sowIndoorsWeeksBefore=2, plantOutWeeksAfter=3,
  water="LOW", tip="Doesn't bolt in summer like true spinach.",
  prep=SOAK("New Zealand spinach", ["The seed is hard so a soak helps it germinate."]),
  pot="9 cm pots", seeds=2, depth="1 cm", germ="14 to 21 days", space=60, row=60,
  harvest=["Pick the young tips regularly through summer."])

P("redonion", "Red onion", "🧅", "VEG", "ALLIUM", 120, 4, directWeeksAfter=-4, frostHardy=True, water="LOW",
  feed="LOW", planted=True, companions=["carrot", "beetroot", "lettuce"], avoid=LEGUMES,
  tip="Heat-treated sets are less likely to bolt.", depth="just the tip showing", space=10, row=30,
  planting=["Push sets into the soil so just the tip shows.", "Space them 10 cm apart in rows 30 cm apart."],
  harvest=["Lift when the leaves fall over and dry in the sun."])

P("elephantgarlic", "Elephant garlic", "🧄", "VEG", "ALLIUM", 270, 3, fixedSow=[10, 15], frostHardy=True,
  tunnel="OUTDOOR", water="LOW", feed="LOW", planted=True, avoid=LEGUMES, tip="Huge mild cloves. Great roasted.",
  depth="10 cm", space=25, row=40,
  planting=["Plant cloves 10 cm deep, pointy end up.", "Space them 25 cm apart in rows 40 cm apart."],
  harvest=["Lift in July when the leaves yellow.", "Dry for a few weeks before storing."])

P("peashoots", "Pea shoots and microgreens", "🌱", "VEG", "LEGUME", 21, 2, directWeeksAfter=-8, frostHardy=True,
  tunnel="LIKES", feed="LOW", potSize="seed tray on a windowsill",
  tip="Grow on a windowsill any time of year. Ready in three weeks.",
  prep=SOAK("pea"), broadcast=True, depth="2 cm", space=2, row=2, layout="A tray full, seeds almost touching.",
  harvest=["Snip when 10 cm tall, just above the lowest leaves. They may regrow once."])

# =====================================================================
# MORE FRUIT
# =====================================================================
P("blackberry", "Thornless blackberry", "🫐", "FRUIT", "OTHER", 250, 6, fixedSow=[11, 20], frostHardy=True,
  tunnel="OUTDOOR", perennial=True, planted=True, solo=True, tip="Thornless types are far nicer to pick.",
  depth="same as in its pot", space=250, row=200,
  planting=["Put up wires on a fence or posts at 90 cm, 1.2 m and 1.5 m.",
            "Plant at the same depth it was growing and tie the canes to the wires."],
  care=["After fruiting cut out the canes that fruited and tie in the new ones."],
  harvest=["Pick when fully black and soft."])

P("tayberry", "Tayberry", "🍇", "FRUIT", "OTHER", 230, 5, fixedSow=[11, 20], frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, tip="A raspberry and blackberry cross with big sweet fruit.",
  depth="same as in its pot", space=250, row=200,
  planting=["Plant against a fence or post and wire support.", "Tie canes in a fan as they grow."],
  care=["Cut out fruited canes after picking."], harvest=["Pick when deep purple-red."])

P("fig", "Fig", "🟤", "FRUIT", "OTHER", 270, 6, fixedSow=[3, 15], perennial=True, planted=True, solo=True,
  tunnel="LIKES", potSize="45 cm pot", tip="Restricting the roots in a pot gives more fruit.",
  depth="same as in its pot", space=200, row=200,
  planting=["Plant in a big pot, or against a sunny wall with the roots boxed in with paving slabs."],
  care=["Protect the young fruitlets with fleece over winter."], harvest=["Pick when soft and drooping on the stalk."])

P("grape", "Grape vine", "🍇", "FRUIT", "OTHER", 270, 4, fixedSow=[12, 1], frostHardy=True, perennial=True,
  planted=True, solo=True, tunnel="NEEDS", tip="A vine in the tunnel or against a sunny wall can last decades.",
  depth="same as in its pot", space=150, row=150,
  planting=["Plant at the side of the tunnel and train the main stem up and along the roof frame on wires."],
  care=["Prune hard in winter, cutting side shoots back to two buds."], harvest=["Pick bunches when sweet."])

P("cherry", "Cherry tree", "🍒", "FRUIT", "OTHER", 200, 3, fixedSow=[12, 1], frostHardy=True, tunnel="OUTDOOR",
  perennial=True, planted=True, solo=True, tip="Stella is self fertile. Net it or the birds get them all.",
  depth="graft union 5 cm above soil", space=400, row=400,
  planting=["Soak bare roots for an hour.", "Drive a stake in first, then plant at the same depth as before.",
            "Firm in well and tie to the stake."],
  care=["Only prune in summer to avoid silver leaf disease."], harvest=["Pick by the stalk when dark and sweet."])

P("alpinestrawberry", "Alpine strawberry", "🍓", "FRUIT", "OTHER", 90, 16, sowIndoorsWeeksBefore=10,
  plantOutWeeksAfter=0, frostHardy=True, perennial=True, feed="LOW", tip="Tiny intense berries all summer. Happy in shade.",
  pot="a seed tray", seeds="a pinch", depth="surface", germ="14 to 28 days", clump=True,
  sow_extra=["Prick out into modules when they have two true leaves."], space=25, row=25,
  layout="Along the edge of a bed as a little border.", harvest=["Pick a few every day when ripe."])

# =====================================================================
# BULBS
# =====================================================================
P("crocus", "Crocus", "💜", "BULB", "OTHER", 160, 4, fixedSow=[9, 20], frostHardy=True, tunnel="OUTDOOR",
  planted=True, perennial=True, water="LOW", feed="LOW", tip="One of the first foods of the year for bumblebees.",
  bloom=["WINTER", "SPRING"], flowers="Feb to Mar", depth="8 cm", space=8, row=8,
  planting=["Plant corms pointy end up, 8 cm deep.", "Scatter handfuls and plant where they land for a natural look."],
  layout="Drifts in grass, under trees or along the front of beds.",
  harvest=["Let the leaves die back naturally so they flower again next year."])

P("snowdrop", "Snowdrop", "🤍", "BULB", "OTHER", 330, 4, fixedSow=[2, 20], frostHardy=True, tunnel="OUTDOOR",
  planted=True, perennial=True, water="LOW", feed="LOW", tip="Plant 'in the green' just after flowering. Dry bulbs often fail.",
  bloom=["WINTER"], flowers="Jan to Feb", depth="same as before, look for the white base of the stem", space=10, row=10,
  planting=["Plant clumps in the green, with leaves still on, at the same depth they were growing.",
            "Water them in. They'll flower next winter."],
  layout="Small clumps under trees and shrubs.", harvest=["Leave them to spread. Split big clumps after flowering."])

P("daffodilmini", "Dwarf daffodil (Tête-à-tête)", "🌼", "BULB", "OTHER", 170, 4, fixedSow=[9, 25], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, water="LOW", feed="LOW", tip="Perfect for pots and window boxes.",
  bloom=["SPRING"], flowers="Mar to Apr", depth="10 cm", space=5, row=5,
  planting=["Plant bulbs pointy end up, 10 cm deep and 5 cm apart."], layout="Packed close together in pots or clumps.",
  harvest=["Leave the leaves for six weeks after flowering."])

P("hyacinth", "Hyacinth", "🪻", "BULB", "OTHER", 170, 3, fixedSow=[10, 15], frostHardy=True, tunnel="OUTDOOR",
  planted=True, water="LOW", feed="LOW", tip="Wear gloves when handling. The bulbs can itch.",
  bloom=["SPRING"], flowers="Mar to Apr", depth="10 cm", space=15, row=15,
  planting=["Plant 10 cm deep and 15 cm apart, pointy end up."], harvest=["Enjoy the scent. Cut a few for indoors."])

P("muscari", "Grape hyacinth (muscari)", "💙", "BULB", "OTHER", 170, 4, fixedSow=[10, 1], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, water="LOW", feed="LOW", tip="Spreads into lovely blue carpets.",
  bloom=["SPRING"], flowers="Mar to May", depth="8 cm", space=8, row=8,
  planting=["Plant 8 cm deep in groups of 20 or more."], harvest=["Leave to spread naturally."])

P("bluebell", "Native bluebell", "🔵", "BULB", "OTHER", 200, 4, fixedSow=[10, 1], frostHardy=True, tunnel="OUTDOOR",
  planted=True, perennial=True, water="LOW", feed="LOW",
  tip="Buy the native Hyacinthoides non-scripta, not Spanish bluebells which cross with wild ones.",
  bloom=["SPRING"], flowers="Apr to May", depth="10 cm", space=10, row=10,
  planting=["Plant 10 cm deep in a shady spot under trees or a hedge."], harvest=["Leave them to naturalise."])

P("alliumbulb", "Ornamental allium", "🟣", "BULB", "ALLIUM", 220, 4, fixedSow=[10, 1], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, water="LOW", feed="LOW", tip="Big purple pompoms that bees adore.",
  bloom=["SUMMER"], flowers="May to Jun", depth="15 cm", space=20, row=20,
  planting=["Plant 15 cm deep, about three times the height of the bulb."],
  harvest=["Leave the seed heads standing. They look great into autumn."])

P("iris", "Dwarf iris (reticulata)", "💜", "BULB", "OTHER", 140, 3, fixedSow=[10, 1], frostHardy=True,
  tunnel="OUTDOOR", planted=True, water="LOW", feed="LOW", tip="Tiny jewel flowers in late winter. Great in pots.",
  bloom=["WINTER"], flowers="Feb to Mar", depth="8 cm", space=5, row=5,
  planting=["Plant 8 cm deep in well-drained soil or gritty compost in pots."], harvest=["Enjoy them close up in a pot."])

P("anemoneblanda", "Wood anemone (blanda)", "🌸", "BULB", "OTHER", 170, 4, fixedSow=[10, 1], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, water="LOW", feed="LOW", tip="Daisy-like flowers that carpet the ground.",
  bloom=["SPRING"], flowers="Mar to Apr", depth="5 cm", space=10, row=10,
  prep=dict(kind="SOAK", leadDays=1, title="Soak anemone tubers", steps=[
      "Soak the knobbly tubers in lukewarm water overnight. They plump up and grow away much faster.",
      "Plant them the next day."]),
  planting=["Plant 5 cm deep. If you can't tell which way is up, plant them on their side."],
  harvest=["Leave them to spread."])

P("camassia", "Camassia (quamash)", "💙", "BULB", "OTHER", 230, 4, fixedSow=[10, 1], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, feed="LOW", potSize="30 cm pot",
  tip="Tall spikes of starry blue flowers in late spring. One of the few bulbs that loves heavy, damp Irish soil.",
  bloom=["SPRING", "SUMMER"], flowers="May to Jun", depth="10 cm", space=15, row=15,
  planting=["Plant from late September to November, before the ground gets cold.",
            "Plant 10 cm deep, pointy end up, in groups of five or more. Sun or light shade suits them.",
            "They're happy in clay and damp spots where tulips would rot, and they look great planted into long grass."],
  care=["Leave the leaves to die back naturally after flowering. They feed next year's flowers.",
        "If you grow them in grass, wait until the leaves have yellowed before mowing, usually by July.",
        "Every four or five years, lift and split crowded clumps in autumn and replant the spare bulbs."],
  harvest=["Cut a few spikes for the vase when the bottom flowers open. They last about a week.",
           "Otherwise just enjoy them. Bees love them in May."])

P("fritillary", "Snake's head fritillary", "🔔", "BULB", "OTHER", 200, 3, fixedSow=[9, 15], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, feed="LOW", tip="Chequered bells that love damp grass.",
  bloom=["SPRING"], flowers="Apr to May", depth="10 cm", space=10, row=10,
  planting=["Plant as soon as you get them. The bulbs dry out fast.", "Plant 10 cm deep in moist soil."],
  harvest=["Let them seed around."])

P("lily", "Lily", "🌺", "BULB", "OTHER", 110, 4, directWeeksAfter=-4, frostHardy=True, tunnel="OUTDOOR",
  planted=True, perennial=True, feed="MEDIUM", potSize="30 cm pot", tip="Watch for the red lily beetle and squash it.",
  bloom=["SUMMER"], flowers="Jun to Aug", depth="15 cm", space=20, row=20,
  planting=["Plant bulbs 15 cm deep in well-drained soil or deep pots.", "Add grit to the bottom of the hole in heavy soil."],
  care=["Stake tall varieties."], harvest=["Cut for the vase and snip off the orange pollen so it doesn't stain."])

P("ranunculus", "Ranunculus", "🌹", "BULB", "OTHER", 90, 6, directWeeksAfter=-4, tunnel="LIKES", planted=True,
  feed="MEDIUM", tip="Gorgeous layered cut flowers. They do best in the tunnel.",
  bloom=["SPRING", "SUMMER"], flowers="May to Jul", depth="5 cm", space=15, row=15,
  prep=dict(kind="SOAK", leadDays=1, title="Soak ranunculus corms", steps=[
      "Soak the claw-like corms in room temperature water for three to four hours. Don't leave them longer or they can rot.",
      "Plant with the claws pointing down."]),
  planting=["Plant 5 cm deep with the claws pointing down."], harvest=["Cut when the buds are coloured and squishy like a marshmallow."])

P("colchicum", "Autumn crocus (colchicum)", "💜", "BULB", "OTHER", 50, 4, fixedSow=[8, 15], frostHardy=True,
  tunnel="OUTDOOR", planted=True, perennial=True, water="LOW", feed="LOW",
  tip="Flowers in autumn before its leaves appear. All parts are poisonous.",
  bloom=["AUTUMN"], flowers="Sep to Oct", depth="10 cm", space=15, row=15,
  planting=["Plant as soon as you get them in August, 10 cm deep."], harvest=["Enjoy the late colour."])

P("cyclamen", "Hardy cyclamen", "🌸", "BULB", "OTHER", 30, 8, fixedSow=[9, 1], frostHardy=True, tunnel="OUTDOOR",
  planted=True, perennial=True, water="LOW", feed="LOW", tip="Cyclamen hederifolium flowers in autumn under trees.",
  bloom=["AUTUMN"], flowers="Sep to Nov", depth="just below the surface", space=15, row=15,
  planting=["Plant the tuber hollow side up, just below the soil surface."], harvest=["Leave them to seed around."])

# =====================================================================
# MORE FLOWERS
# =====================================================================
P("sweetwilliam", "Sweet William", "🌸", "FLOWER", "OTHER", 300, 8, fixedSow=[6, 1], frostHardy=True, feed="LOW",
  tip="A biennial. Sow in early summer for flowers next year.", depth="0.5 cm", space=25, row=25, germ="7 to 14 days",
  care=["Move to where they'll flower in autumn."], harvest=["Cut for scented posies."])

P("scabious", "Scabious", "💜", "FLOWER", "OTHER", 90, 14, sowIndoorsWeeksBefore=6, plantOutWeeksAfter=2,
  feed="LOW", tip="A butterfly favourite and a great cut flower.", pot="modules", seeds=2, depth="0.5 cm",
  germ="10 to 20 days", space=30, row=30, harvest=["Cut often to keep the flowers coming."])

P("verbena", "Verbena bonariensis", "💜", "FLOWER", "OTHER", 120, 14, sowIndoorsWeeksBefore=10, plantOutWeeksAfter=2,
  water="LOW", feed="LOW", tip="Tall airy purple flowers that butterflies can't resist.",
  prep=CHILL("verbena", 2), pot="a seed tray", seeds="a pinch", depth="surface", germ="14 to 28 days at 21°C", clump=True,
  sow_extra=["Prick out into modules once big enough."], space=30, row=30,
  layout="Dotted through other plants. You can see through it so it works at the front too.",
  harvest=["Leave the stems standing in winter for the birds."])

P("sedum", "Sedum (ice plant)", "🌸", "FLOWER", "OTHER", 120, 10, directWeeksAfter=2, frostHardy=True, perennial=True,
  planted=True, water="LOW", feed="LOW", tip="Flat pink heads covered in bees and butterflies in September.",
  depth="same as in its pot", space=40, row=40, planting=["Plant in full sun at the same depth as in its pot."],
  care=["Cut back by a third in late May (the Chelsea chop) to stop it flopping."], harvest=["Leave the seed heads for winter."])

P("aster", "Michaelmas daisy", "💜", "FLOWER", "DAISY", 150, 8, directWeeksAfter=2, frostHardy=True, perennial=True,
  planted=True, feed="LOW", tip="Late nectar for bees getting ready for winter.",
  depth="same as in its pot", space=45, row=45, planting=["Plant at the same depth it was growing."],
  care=["Split clumps every few years in spring."], harvest=["Enjoy the autumn colour."])

P("heather", "Winter heather", "💗", "FLOWER", "OTHER", 200, 12, fixedSow=[9, 15], frostHardy=True, perennial=True,
  planted=True, water="LOW", feed="LOW", tip="Erica carnea flowers through winter when little else does.",
  depth="same as in its pot", space=40, row=40, planting=["Plant in ericaceous or ordinary soil at the same depth as before."],
  care=["Trim lightly after flowering."], harvest=["Enjoy flowers through the dark months."])

P("cranesbill", "Hardy geranium (cranesbill)", "💜", "FLOWER", "OTHER", 60, 14, directWeeksAfter=0, frostHardy=True,
  perennial=True, planted=True, feed="LOW", tip="Tough, long flowering and loved by bees. Grows in sun or shade.",
  depth="same as in its pot", space=45, row=45, planting=["Plant at the same depth it was growing."],
  care=["Shear back after the first flush for more flowers."], harvest=["Enjoy."])

P("honesty", "Honesty", "💜", "FLOWER", "BRASSICA", 300, 6, fixedSow=[6, 1], frostHardy=True, feed="LOW",
  tip="A biennial with silver seed discs for drying.", depth="1 cm", space=30, row=30, germ="10 to 14 days",
  harvest=["Cut the seed heads in autumn and peel back the outer skins to reveal the silver discs."])


P("banana", "Banana (dwarf, in a pot)", "🍌", "FRUIT", "OTHER", 730, 4, directWeeksAfter=2, perennial=True,
  planted=True, potOnly=True, solo=True, tunnel="NEEDS", water="HIGH", feed="HIGH", potSize="45 cm pot or bigger",
  tip="Ireland is too cool for bananas to fruit outside. A dwarf Cavendish in a big pot in a warm conservatory or bright room can fruit after two or three years. The hardy Musa basjoo grows outside in sheltered gardens but its fruit isn't worth eating.",
  depth="same as in its pot", space=100, row=100,
  planting=["Buy a young dwarf Cavendish plant. It's far quicker than seed and the fruit will be edible.",
            "Pot it into a large pot with drainage holes, using multipurpose compost mixed with some John Innes No 3 and a handful of grit.",
            "Plant at the same depth it was growing and stand it in the brightest, warmest spot you have, ideally 20 to 30°C and never below 10°C."],
  layout="One plant per pot. Give it room, as the leaves spread over a metre.",
  care=["Water generously in summer so the compost never dries out, and mist the leaves on warm days.",
        "Feed every week from spring to autumn with a high nitrogen liquid feed, then switch to tomato feed once a flower stalk appears.",
        "Move it outside to a sheltered sunny spot only in warm summer spells and bring it back in before nights drop below 10°C.",
        "In winter water much less and keep it somewhere bright that stays above 10°C."],
  harvest=["After two or three years a big purple flower stalk appears and small green bananas form behind it. They take three to six months to fill out.",
           "Cut the whole bunch when the fruit is plump and the first one starts to turn yellow, then let them ripen indoors.",
           "The stem that fruited then dies. Cut it down and let one of the young side shoots take its place."])
SEED_OVERRIDES["banana"] = dict(difficulty="Easy", timing="NONE", years=0, steps=[
    "Bananas you eat have no viable seeds. New plants come from the side shoots (suckers) around the base.",
    "In spring, when a sucker is about 60 cm tall and has its own roots, cut it away from the parent with a sharp spade.",
    "Pot it up on its own and keep it warm until it settles in."])

P("avocado", "Avocado (in a pot)", "🥑", "FRUIT", "OTHER", 1095, 4, directWeeksAfter=2, perennial=True,
  planted=True, potOnly=True, solo=True, tunnel="NEEDS", water="MEDIUM", feed="MEDIUM", potSize="40 cm pot or bigger",
  tip="Ireland is too cool and dull for avocados to crop outside. Grow one as a leafy tree in a pot in a bright room or conservatory. A grafted plant may give a few fruit after three or four years but don't count on it. One grown from a stone makes a lovely houseplant but will rarely fruit.",
  depth="same as in its pot", space=100, row=100,
  planting=["For any chance of fruit buy a young grafted plant of a named variety such as Hass. A stone from the shop is fun to grow but takes many years and rarely fruits.",
            "Plant it in a pot with plenty of drainage holes, using multipurpose compost mixed with John Innes No 3 and a couple of handfuls of grit or perlite.",
            "Plant at the same depth it was growing and stand it in the brightest spot you have, out of cold draughts and away from radiators."],
  layout="One plant per pot. It will grow into a small tree, so move it up a pot size every couple of years.",
  care=["Water when the top few centimetres of compost feel dry, then let the excess drain away. Avocados hate soggy roots.",
        "Feed every two weeks from spring to late summer with a balanced liquid feed. Stop feeding in winter.",
        "Pinch out the tip when it reaches about 30 cm, and again later, to make it bushy instead of tall and spindly.",
        "It can go outside to a sheltered sunny spot in warm summer spells. Bring it back in before nights drop below 10°C.",
        "In winter keep it bright, above 10°C and on the dry side. Brown leaf tips usually mean dry air or salty tap water, so use rainwater when you can."],
  harvest=["Flowers appear in spring but fruit only sets now and then indoors. Hand pollinating with a soft brush may help.",
           "Fruit takes six months or more to fill out and does not ripen on the tree.",
           "Pick when full sized and the skin has lost its shine, then leave on a windowsill for a week or two until it gives slightly when pressed."])
SEED_OVERRIDES["avocado"] = dict(difficulty="Easy", timing="NONE", years=0, steps=[
    "Avocados don't come true from the stone and seedlings take ten years or more to fruit, if they ever do. Grow one just for the leaves.",
    "Wash a stone from a ripe avocado and push three cocktail sticks into its sides. Rest it on a glass of water with the pointed end up and the bottom third under water.",
    "Keep it somewhere warm and change the water every few days. Roots and a shoot appear in four to eight weeks, sometimes longer.",
    "When the shoot is about 15 cm tall pot it into compost with the top half of the stone above the surface."])

# ---------------------------------------------------------------------
# Bulbs and tubers listed earlier move into the bulbs category
for _p in PLANTS:
    if _p["id"] in ("tulip", "daffodil", "gladiolus", "dahlia"):
        _p["category"] = "BULB"


def main(out_paths):
    ids = [p["id"] for p in PLANTS]
    assert len(ids) == len(set(ids)), "duplicate ids"
    known = set(ids)
    for p in PLANTS:
        for ref in p.get("companions", []) + p.get("avoid", []):
            if ref not in known:
                print("unknown reference", p["id"], "->", ref, file=sys.stderr)
    data = dict(version=VERSION, updated=UPDATED, plants=[build(p) for p in PLANTS])
    text = json.dumps(data, ensure_ascii=False, indent=1)
    for path in out_paths:
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)
        # A tiny file the app checks first, so it only downloads the full database when it has changed.
        with open(path.rsplit("/", 1)[0] + "/version.json" if "/" in path else "version.json", "w") as f:
            json.dump(dict(version=VERSION, updated=UPDATED, plants=len(PLANTS)), f)
    print(len(PLANTS), "plants,", len(text) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1:])
