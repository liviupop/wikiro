﻿#!/usr/bin/python
# -*- coding: utf-8  -*-

#
# (C) Strainu, 2012-2015
#
# Distributed under the terms of the GPLv2 license.
#
#
import re
import collections

class Changes:
	none	= 0x000
	article = 0x010
	coord   = 0x020
	image   = 0x040
	creator = 0x080
	commons = 0x100
	other	= 0x200
	all	= 0xFFF


lmi_blacklist = [#all lowercase
		u'detali',#detaliu, detalii
		u'pisani',#pisanie, pisanii
		u'interio',#interior, interioare
		u'plac',#placa, placă
		u'usa',
		u'fereastr',
		u'logo',#logo
		u'icon',
		u'inscrip',#incriptie, incription
		u'fresc',#frescă, fresco
	    ]

plan = 	[#all lowercase
		u'.svg',#svg files are definetely not pictures
		u'schem',
		u'plan',#plans are plans
		u'v1',
		u'v2',
		u'reconstituire',
		u'3d',
		u'localizare',
		u'map'
		u'layout',
		u'grundriss',
		u'schita',
		u'schiță',
		u'schița',
	]

config = {
	'wikidata':
	{
		'lmi': 'P1770',
		'ran': 'P2845',
	},
	'ro':
	{
		'lmi':#database we work on
		{
			#where to search for information
			'project' : 'wikipedia',
			'lang' : 'ro',
			'namespaces': [0, 6],
			#'namespaces': [6],
			'listNamespaces': [0],
			'pagePrefix': {
				'Lista monumentelor istorice din județul',
				'Lista monumentelor istorice din București',
			},
			'codeRegexp' : "(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))",
			'codeRegexpCompiled': re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
            'templateRegexpCompiled': re.compile("\{\{(?:[a-z]*codLMI|Monument istoric)\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
			'codeTemplate': ["codLMI", "Monument istoric"],
			'codeTemplateParams': 
			[
			],
			#list params
			'headerTemplate' : u'ÎnceputTabelLMI',
			'rowTemplate' : u'ElementLMI',
			'footerTemplate' : u'SfârșitTabelLMI',
			'fields' : collections.OrderedDict([
						(u'Cod', {'code': Changes.all, }),
						(u'NotăCod', {'code': Changes.all, }),
						(u'RefCod', {'code': Changes.all, }),
						(u'FostCod', {'code': Changes.other, }),
						(u'CodRan', {'code': Changes.other, 'alias': 'ran', }),
						(u'Cod92', {'code': Changes.other, 'alias': 'lmi92', }),
						(u'Denumire', {'code': Changes.article, }),
						(u'Localitate', {'code': Changes.all, }),
						(u'Adresă', {'code': Changes.all, }),
						(u'Datare', {'code': Changes.all, }),
						(u'Creatori', {'code': Changes.creator, }),
						(u'Lat', {'code': Changes.coord, }),
						(u'Lon', {'code': Changes.coord, }),
						(u'OsmLat', {'code': Changes.coord, }),
						(u'OsmLon', {'code': Changes.coord, }),
						(u'Imagine', {'code': Changes.image, 'blacklist': lmi_blacklist + plan}),
						(u'Plan', {'code': Changes.image, 'blacklist': lmi_blacklist}),
						(u'Commons', {'code': Changes.commons, }),
						(u'Copyright', {'code': Changes.creator, }),
					]),
			'idField': u'Cod',
			'keepEmptyFields': False,
			#coordinates params
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'ran':#database we work on
		{
			#where to search for information
			'project' : 'wikipedia',
			'lang' : 'ro',
			'namespaces': [0, 6],
			#'namespaces': [6],
			'listNamespaces': [0],
			'pagePrefix': {
				'Lista siturilor arheologice din județul',
				'Lista siturilor arheologice din București',
			},
			'codeRegexp': "([0-9]{4,6}(\.[0-9][0-9]){1,3})",
			'codeRegexpCompiled': re.compile("([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'templateRegexpCompiled': re.compile("\{\{codRAN\|([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'codeTemplate': ["codRAN"],
			'codeTemplateParams': 
			[
			],
			#list params
			'headerTemplate' : u'ÎnceputTabelRAN',
			'rowTemplate' : u'ElementRAN',
			'footerTemplate' : u'SfârșitTabelRAN',
			'fields' : collections.OrderedDict([
						(u'Cod', {'code': Changes.all, }),
						(u'NotăCod', {'code': Changes.all, }),
						(u'CodLMI', {'code': Changes.other, }),
						(u'Index', {'code': Changes.all, }),
						(u'Nume', {'code': Changes.article, }),
						(u'NumeAlternative', {'code': Changes.all, }),
						(u'Categorie', {'code': Changes.all, }),
						(u'TipMonument', {'code': Changes.all, }),
						(u'TipCod', {'code': Changes.all, }),
						(u'CodSIRUTA', {'code': Changes.all, }),
						(u'Localitate', {'code': Changes.all, }),
						(u'Adresă', {'code': Changes.all, }),
						(u'Cultura', {'code': Changes.all, }),
						(u'Faza', {'code': Changes.all, }),
						(u'Datare', {'code': Changes.all, }),
						(u'Descoperitor', {'code': Changes.creator, }),
						(u'Descoperit', {'code': Changes.all, }),
						(u'Stare', {'code': Changes.all, }),
						(u'Lat', {'code': Changes.coord, }),
						(u'Lon', {'code': Changes.coord, }),
						(u'Latd', {'code': Changes.coord, }),
						(u'Latm', {'code': Changes.coord, }),
						(u'Lats', {'code': Changes.coord, }),
						(u'Lond', {'code': Changes.coord, }),
						(u'Lonm', {'code': Changes.coord, }),
						(u'Lons', {'code': Changes.coord, }),
						(u'Imagine', {'code': Changes.image, 'blacklist': lmi_blacklist + plan}),
						(u'Commons', {'code': Changes.commons, }),
					]),
			'idField': u'Cod',
			'keepEmptyFields': False,
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'wlemd':#database we work on
		{
			'project' : 'wikipedia',
			'lang' : 'ro',
			'namespaces': [0],
			'listNamespaces': [4],
			'pagePrefix': {
				'Wikipedia:Wiki Loves Earth/Moldova/Lista',
			},
			'codeRegexpCompiled': re.compile("((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'templateRegexpCompiled': re.compile("\{\{Monument natural MD\|((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'codeTemplate': ["Monument natural MD"],
			'codeTemplateParams': 
			[
			],
			'headerTemplate' : u'Wikipedia:Wiki Loves Earth/Moldova/start',
			'rowTemplate' : u'Wikipedia:Wiki Loves Earth/Moldova/item',
			'footerTemplate' : u'Wikipedia:Wiki Loves Earth/Moldova/end',
			'codeRegexp' : "((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))",
			'fields' : collections.OrderedDict([
							(u'ID', {'code': Changes.all, }),
							(u'denumire', {'code': Changes.article, }),
							(u'proprietar', {'code': Changes.all, }),
							(u'descriere', {'code': Changes.all, }),
							(u'raion', {'code': Changes.all, }),
							(u'ascunde_raion', {'code': Changes.all, }),
							(u'amplasament', {'code': Changes.all, }),
							(u'suprafata', {'code': Changes.all, }),
							(u'latitudine', {'code': Changes.coord, }),
							(u'longitudine', {'code': Changes.coord, }),
							(u'tip', {'code': Changes.all, }),
							(u'subtip', {'code': Changes.all, }),
							(u'imagine', {'code': Changes.image, 'blacklist': []}),
							(u'categorie', {'code': Changes.commons, }),
				            (u'q', {'code': Changes.all, }),
	                        (u'wikilink', {'code': Changes.all, }),
	                        (u'numar', {'code': Changes.all, }),
						]),
			'idField': u'ID',
			'keepEmptyFields': True,
			'geolimits': {
				'north': 48.5,
				'south': 45.4,
				'west':  26.6,
				'east':  30.2,
			},
		},
		'infoboxes':
		[
		{
			'name': 'Infocaseta Monument|Cutie Monument',
			'author': ['artist', 'artist1', 'artist2', 'arhitect'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod2',#TODO: this is a hack, we probably need to duplicate the entry
			'lmi': 'cod',
		},
		{
			'name': 'Clădire Istorică',
			'author': ['arhitect'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod-ran',
			'lmi': 'cod-lmi',
		},
		{
			'name': 'Cutie Edificiu Religios|Infocaseta Edificiu religios|Infocaseta Teatru|Moschee',
			'author': ['arhitect'],
			'image': 'imagine',
			# the databases we work on
			'ran': '',#nada yet
			'lmi': '',
		},
		{
			'name': 'Castru|Infocaseta Castru|Infocaseta Villa rustica',
			'author': [],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod RAN',
			'lmi': 'cod LMI',
		},
		{
			'name': 'Infocasetă Davă|Infocaseta Davă|Infocaseta Cetate dacică',
			'author': [],
			'image': 'imagine',
			# the databases we work on
			'ran': 'ref:RO:RAN',
			'lmi': 'ref:RO:LMI',
		},
		{
			'name': 'Infocaseta Gară|Infocaseta Muzeu',
			'author': [],
			'image': 'imagine',
			# the databases we work on
			'ran': '',
			'lmi': '',
		},
		{
			'name': 'Infocaseta Biserică din lemn',
			'author': ['meșteri', 'zugravi'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'cod RAN',
			'lmi': 'cod LMI',
		},
		{
			'name': 'Infocaseta Lăcaș de cult|Mănăstire',
			'author': ['arhitect', 'constructor', 'pictor'],
			'image': 'imagine',
			# the databases we work on
			'ran': 'codRAN',
			'lmi': 'codLMI',
		},
		{
			'name': 'Infocaseta clădire|Infobox cladire|Infobox building',
			'author': ['arhitect', 'firma_arhitectura', 'inginer', 'alti_designeri'],
			'image': 'image',
			# the databases we work on
			'ran': '',#nada yet
			'lmi': '',
		},
		],
		'qualityTemplates':
		[
			'Articol bun',
			'Articol de calitate',
			'Listă de calitate',
		],
	},
	'commons':
	{
		'lmi':
		{
			'namespaces': [14, 6],
			#'namespaces': [14],
			'codeRegexpCompiled': re.compile("(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
            'templateRegexpCompiled': re.compile("\{\{(?:Monument istoric|codLMI)\|(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))", re.I),
			'codeTemplate': ["Monument istoric", "Monumente istorice", "codLMI"],
			'codeTemplateParams': 
			[
				'lmi92',
				'ran',
				'eroare',
			],
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'ran':#database we work on
		{
			'namespaces': [14, 6],
			#'namespaces': [6],
			'codeRegexpCompiled': re.compile("([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'templateRegexpCompiled': re.compile("\{\{codRAN\|([0-9]{4,6}(\.[0-9][0-9]){1,3})", re.I),
			'codeTemplate': ["codRAN", "RAN"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.3,
				'south': 43.6,
				'west':  20.27,
				'east':  29.7,
			},
		},
		'wlemd':#database we work on
		{
			'namespaces': [14, 6],
			'codeRegexpCompiled': re.compile("((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'templateRegexpCompiled': re.compile("\{\{Monument natural MD\|((MD)-([a-z]{1,2})-([a-z]{2,3}(\.[a-z]{1,2})?)-([0-9]+))", re.I),
			'codeTemplate': ["Monument natural MD"],
			'codeTemplateParams': 
			[
			],
			'geolimits': {
				'north': 48.5,
				'south': 45.4,
				'west':  26.6,
				'east':  30.2,
			},
		},
		'infoboxes': 
		[
			{
				#the format is actually {{Creator:Name}} without parameters
				'name': 'Creator',
				'author': ['_name'],
				'image': 'imagine',
				# the databases we work on
				'ran': '',
				'lmi': '',
			},
			{
				'name': 'codLMI|Monument istoric',
				'author': [],
				'image': 'imagine',
				# the databases we work on
				'ran': 'ran',
				'lmi': '1',#TODO
			},
		],
		'qualityTemplates':
		[
			'Valued image',
			'QualityImage',
			'Assessments',
			'Wiki Loves Monuments 2011 Europe nominee',
			'WLM finalist or winner image 2012',
			'WLM finalist or winner image',
			'Picture of the day',
			'Media of the day',
		],
		'validOccupations':
		{
			#we don't care about the creators of the 2D representation
			'architect': 'arhitect',
			'architectural painter': 'pictor arhitectural',
			'artist': 'artist',
			'artisan': 'artizan',
			'author': 'autor',
			'carpenter': 'tâmplar',
			'decorator': 'decorator',
			'engineer': 'inginer',
			'entrepreneur': 'întreprinzător',
			'ornamental painter': 'pictor ornamental',
			'sculptor': 'sculptor',
		},
	}
}


def filterOne(contents, countryconfig):
	'''
	Any kind of filtering of the data of a monument should happen in this functions
	'''
	#if countryconfig.get('idField') == 'ID': #WLEMD
	#	contents[countryconfig.get('idField')] = "-".join([u"MD", contents['raion'], contents['tip'], "%03d" % int(contents['ID'])])
	return contents
