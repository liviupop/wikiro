#!/usr/bin/python
# -:- coding: utf8 -:-

#
# (C) Strainu 2016
#
# Distributed under the terms of the MIT license.
#
import pywikibot
from pywikibot import i18n, config, pagegenerators, textlib, weblib

import sys
import csv
import json
sys.path.append("wikiro/robots/python/pywikipedia")
import strainu_functions as sf
from wikidata import robot_romania as robot

config = {
	'properties':	{
			u'Denumire': ('', None, 'label'),
			u'Coord': ('P625', False, 'globe-coordinate'),
			u'Imagine': ('P18', True, 'commonsMedia'),
                        u'Țară': ('P17', False, 'wikibase-item'),
			u'Commons': ('P373', False, 'string'),
			u'SIRUTA': ('P843', False, 'string'),
			}
}
    
class CityData(robot.WorkItem):
    def __init__(self, config):
        self.db = {}
        self.always = False
        self.config = config

    def userConfirm(self, question):
        """Obtain user response."""
        if self.always:
            return True

        choice = pywikibot.input_choice(question,
                                        [('Yes', 'y'),
                                         ('No', 'N'),
                                         ('Always', 'a')],
                                        default='N')

        if choice == 'n':
            return False

        if choice == 'a':
            self.always = True

        return True

    def updateProperty(self, item, key, data):
        try:
            prop, pref, datatype = self.config["properties"][key]
            if prop in item.claims:
                #don't bother about those yet
                pywikibot.output(u"Wikidata already has %s: %s" % (key, item.claims[prop][0].getTarget()))
                pass
            else:
                if datatype == 'wikibase-item':
                    page = pywikibot.Page(pywikibot.Site(), data[key])
                    while page.isRedirectPage():
                        page = page.getRedirectTarget()
                    val = page.data_item()
                    desc = page.title()
                elif datatype == 'globe-coordinate':
                    val = pywikibot.Coordinate(lat=float(data[key][0]),
                                         lon=float(data[key][1]),
                                         globe='earth',
                                         precision=0.001
                                         )
                    desc = data[key]
                elif datatype == 'commonsMedia':
                    val = pywikibot.FilePage(pywikibot.Site('commons', 'commons'), u"File:" + sf.stripNamespace(data[key]))
                    while val.isRedirectPage():
                        val = val.getRedirectTarget()
                    desc = val.title()
                    if not val.exists():
                        raise ValueError("Local image given")
                else:
                    val = desc = data[key]
                claim = pywikibot.Claim(item.repo, prop, datatype=datatype)
                claim.setTarget(val)
                answer = self.userConfirm("Update element %s with %s \"%s\"?" % (item.labels['ro'], key, desc))
                if answer:
                    item.addClaim(claim)
                    if pref:
                        claim.changeRank('preferred')
        except Exception as e:
            pywikibot.output(e)
            import traceback
            traceback.print_exc()
            pywikibot.output(u"Could not update " + item.labels.get('ro'))

    def updateWikidata(self, item, data):
        #self.updateProperty(item, key, data)
        pass

    def isOfType(self, item, typeName):
        for claim in (item.claims.get(u"P31") or []):
            if claim.getTarget().title() == typeName:
                return True
        return False

    def isCounty(self, item):
        return self.isOfType(item, u"Q1776764")

    def isMunicipality(self, item):
        return self.isOfType(item, u"Q640364")

    def isCity(self, item):
        return self.isOfType(item, u"Q16858213")

    def createCommonsProperty(self, item, siruta):
        if self.isCounty(item) and 'ro' in item.labels and item.labels.get('ro') != u"București":
             self.updateProperty(item, u"Commons", {u"Commons": item.labels.get('ro') + u" County"})
        #TODO: search for it first
        pass

    def sortFromName(self, name):
        return name.replace(u"ș", u"sș").replace(u"ț", u"tț").replace(u"Ș", u"SȘ").replace(u"Ț", u"TȚ").replace(u"ă", u"aă").replace(u"Ă", u"AĂ").replace(u"â", u"aâ").replace(u"Â", u"AÂ").replace(u"î", u"iî").replace(u"Î", u"IÎ")

    def createCountySubcategories(self, county):
        cats = {
            u"Administrative divisions of %s County": [u"Category:Administrative divisions of Romania by county", ], 
            u"Cities and towns in %s County": [u"Category:Cities in Romania by county"], 
            u"Communes in %s County": [u"Category:Communes in Romania"], 
            u"Villages in %s County": [u"Category:Villages in Romania by county"],
        }
        for template in cats:
            cat = template % county
            site = pywikibot.Site("commons", "commons")
            page = pywikibot.Page(site, cat, ns=14)
            if page.exists():
                continue
            text = u"[[Category:%s County]]\n" % county
            for newcat in cats[template]:
                text += u"[[%s|%s]]" % (newcat, self.sortFromName(county))
            print text
            answer = self.userConfirm("Create category %s?" % page.title())
            if answer:
                page.put(text, u"Creating new subcategory for a Romanian County")
            


    def updateCommons(self, item):
        cProp, cPref, cDatatype = self.config["properties"][u"Commons"]
        sProp, sPref, sDatatype = self.config["properties"][u"SIRUTA"]
        if sProp not in item.claims:
            pywikibot.error(u"%s does not have a SIRUTA code" % item.labels.get('ro'))
            return
        elif len(item.claims[sProp]) > 1:
            pywikibot.error(u"%s has several SIRUTA codes" % item.labels.get('ro'))
            return

        if self.isCounty(item) and 'ro' in item.labels and item.labels.get('ro') != u"București":
            self.createCountySubcategories(item.labels.get('ro'))

        if cProp not in item.claims:
            self.createCommonsProperty(item, item.claims[sProp][0].getTarget())
        elif len(item.claims[cProp]) > 1:
            pywikibot.error(u"%s has several Commons categories" % item.labels.get('ro'))
	else:
            self.updateCommonsCat(item, cProp, sProp)
            pass

    def updateCommonsCat(self, item, cProp, sProp):
        cat = item.claims[cProp][0].getTarget()
        siruta = item.claims[sProp][0].getTarget()
        site = pywikibot.Site("commons", "commons")
        page = pywikibot.Page(site, cat, ns=14)
        sirutaTl = pywikibot.Page(site, "SIRUTA", ns=10)
        if not page.exists():
            pywikibot.error(u"[[:commons:Category:%s]] does not exist" % cat)
        return
        while page.isRedirectPage():
            page = page.getRedirectTarget()
        print sirutaTl
        if sirutaTl not in page.getTemplates():
            text = page.get()
            newText = u"{{%s|%s}}\n%s" % (sirutaTl.title(withNamespace=False), siruta, text)
            pywikibot.showDiff(text, newText)
            answer = self.userConfirm("Add SIRUTA code \"%s\" to the category %s?" % (siruta, page.title()))
            if answer:
                page.put(newText, u"Adding SIRUTA code")


    def doWork(self, page, item):
        try:
            self.updateCommons(item)
        except Exception as e:
            pywikibot.output(e)
            import traceback
            traceback.print_exc()
            pywikibot.output(u"Failed to update city data to Wikidata, skipping...")

    def invalidArea(self, item):
        pywikibot.output(u"Country not set, setting...")
        self.updateProperty(item, u"Țară", {u"Țară": u"România"})
        
    def description(self):
        return u"Updating city data on Wikidata"

if __name__ == "__main__":
    pywikibot.handle_args()
    #page = pywikibot.Page(pywikibot.Site(), "P843", ns=120)
    page = pywikibot.Page(pywikibot.Site(), "Q1776764", ns=0)#counties
    generator = pagegenerators.ReferringPageGenerator(page)
    bot = robot.WikidataBot(site=True, generator = generator)

    bot.workers.append(CityData(config))
    bot.run()
