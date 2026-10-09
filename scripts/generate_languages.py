"""Generate original lightweight TextMate grammars and Kotlin language catalog."""
import json, re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main/assets/textmate'
# Lightweight syntax grammars, not complete parsers or language servers.
languages = [
('python','Python','py pyw','False None True and as assert async await break class continue def del elif else except finally for from global if import in is lambda nonlocal not or pass raise return try while with yield print range len str int list dict set tuple open input'),
('javascript','JavaScript','js mjs cjs jsx','async await break case catch class const continue debugger default delete do else export extends false finally for from function if import in instanceof let new null of return static super switch this throw true try typeof var void while yield console'),
('typescript','TypeScript','ts tsx','abstract any as async await boolean break case catch class const constructor continue declare default else enum export extends false finally for from function if implements import in interface keyof let namespace never new null number of private protected public readonly return static string super switch this throw true try type typeof undefined unknown var void while yield console'),
('html','HTML','html htm','html head body div span script style link meta title input button form section article main nav header footer a img p'),
('css','CSS','css scss less','color background display flex grid margin padding border width height position absolute relative font-size font-family align-items justify-content'),
('php','PHP','php','abstract array as break callable case catch class clone const continue declare default die do echo else elseif empty endfor endforeach endif eval exit extends final finally fn for foreach function global if implements include include_once instanceof interface isset list match namespace new null print private protected public readonly require require_once return static switch throw trait try unset use var while yield'),
('c','C','c h','auto break case char const continue default do double else enum extern float for goto if inline int long register restrict return short signed sizeof static struct switch typedef union unsigned void volatile while'),
('cpp','C++','cpp cc cxx hpp hh hxx','alignas alignof asm auto bool break case catch char class concept const constexpr consteval continue co_await co_return co_yield decltype default delete do double else enum explicit export extern false float for friend if inline int long mutable namespace new noexcept nullptr operator override private protected public return short signed sizeof static struct switch template this throw true try typedef typename union unsigned using virtual void volatile while'),
('csharp','C#','cs','abstract as async await base bool break byte case catch char checked class const continue decimal default delegate do double else enum event explicit extern false finally fixed float for foreach if implicit in int interface internal is lock long namespace new null object operator out override params private protected public readonly ref return sbyte sealed short sizeof stackalloc static string struct switch this throw true try typeof uint ulong unchecked unsafe ushort using var virtual void volatile while yield'),
('java','Java','java','abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for if implements import instanceof int interface long native new null package private protected public return short static strictfp super switch synchronized this throw throws transient true false try void volatile while var record'),
('kotlin','Kotlin','kt kts','abstract actual annotation as break by catch class companion const constructor continue data do else enum expect external false final finally for fun if import in infix init inline inner interface internal is lateinit noinline null object open operator out override package private protected public reified return sealed suspend super tailrec this throw true try typealias val var vararg when where while println'),
('rust','Rust','rs','as async await break const continue crate dyn else enum extern false fn for if impl in let loop match mod move mut pub ref return self Self static struct super trait true type unsafe use where while'),
('go','Go','go','break case chan const continue default defer else fallthrough for func go goto if import interface map package range return select struct switch type var nil true false'),
('swift','Swift','swift','actor as associatedtype async await break case catch class continue default defer deinit do else enum extension fallthrough false fileprivate for func guard if import in init inout internal is let nil open operator private protocol public repeat rethrows return self static struct subscript super switch throw throws true try typealias var where while'),
('dart','Dart','dart','abstract as assert async await break case catch class const continue covariant default deferred do dynamic else enum export extends extension external factory false final finally for Function get hide if implements import in interface is late library mixin new null on operator part required rethrow return set show static super switch sync this throw true try typedef var void while with yield'),
('bash','Bash','sh bash zsh','if then else elif fi for in do done while until case esac function select time export local readonly return exit echo printf cd pwd mkdir rm cp mv chmod'),
('powershell','PowerShell','ps1 psm1 psd1','begin break catch class continue data do dynamicparam else elseif end enum exit filter finally for foreach from function if in param process return switch throw trap try until using var while Write-Host Get-Item Set-Content'),
('sql','SQL','sql','SELECT FROM WHERE JOIN LEFT RIGHT INNER OUTER ON GROUP BY ORDER HAVING LIMIT INSERT INTO VALUES UPDATE SET DELETE CREATE TABLE INDEX DROP ALTER DISTINCT AS AND OR NOT NULL PRIMARY KEY REFERENCES UNION WITH select from where join left right inner outer on group by order having limit insert into values update set delete create table index drop alter distinct as and or not null'),
('json','JSON','json jsonc','true false null'),
('yaml','YAML','yaml yml','true false null yes no on off'),
('xml','XML','xml svg plist','xml version encoding'),
('markdown','Markdown','md markdown',''),
('ruby','Ruby','rb','begin break case class def do else elsif end ensure false for if in module next nil rescue return self super then true unless until when while yield'),
('lua','Lua','lua','and break do else elseif end false for function goto if in local nil not or repeat return then true until while'),
('r','R','r R','if else repeat while function for in next break TRUE FALSE NULL NA'),
('toml','TOML','toml','true false'),
('dockerfile','Dockerfile','dockerfile','FROM RUN COPY ADD CMD ENTRYPOINT ENV ARG WORKDIR USER EXPOSE VOLUME LABEL ONBUILD HEALTHCHECK SHELL'),
]
assets.mkdir(parents=True, exist_ok=True)
registry = []
for id, name, extensions, keywords in languages:
    comment = r'#.*$' if id in {'python','bash','powershell','yaml','ruby','r','toml','dockerfile'} else r'--.*$' if id in {'sql','lua'} else r'//.*$'
    patterns = [ {'name': 'comment.line', 'match': comment},
        {'name':'comment.block','begin':r'/\*','end':r'\*/'},
        {'name':'string.quoted.double','begin':'"','end':'"','patterns':[{'name':'constant.character.escape','match':r'\\.'}]},
        {'name':'string.quoted.single','begin':"'",'end':"'",'patterns':[{'name':'constant.character.escape','match':r'\\.'}]},
        {'name':'constant.numeric','match':r'\b(?:0[xX][0-9a-fA-F]+|\d+(?:\.\d+)?)\b'} ]
    if id == 'python':
        patterns.insert(2, {'name':'string.quoted.multi','begin':'"""','end':'"""'})
        patterns.insert(2, {'name':'string.quoted.multi','begin':"'''",'end':"'''"})
    if keywords:
        patterns.append({'name':'keyword.control','match':r'\b(?:'+'|'.join(re.escape(k) for k in keywords.split())+r')\b'})
    patterns.extend([{'name':'entity.name.function','match':r'\b[A-Za-z_][A-Za-z0-9_]*(?=\s*\()'},
        {'name':'variable.other','match':r'\$[A-Za-z_][A-Za-z0-9_]*'},
        {'name':'keyword.operator','match':r'[+*/%=!<>|&:-]+'}])
    if id in {'html','xml'}:
        patterns = [{'name':'comment.block','begin':'<!--','end':'-->'},
                    {'name':'entity.name.tag','match':r'</?[A-Za-z][A-Za-z0-9:-]*|/?>'},
                    {'name':'entity.other.attribute-name','match':r'[A-Za-z-]+(?=\s*=)'}, *patterns]
    if id == 'markdown':
        patterns = [{'name':'markup.heading','match':r'^#{1,6}\s.*$'},
                    {'name':'markup.bold','match':r'\*\*[^*]+\*\*'},
                    {'name':'string.quoted','begin':'`','end':'`'},
                    {'name':'markup.underline.link','match':r'\[[^\]]+\]\([^)]*\)'}]
    (assets / (id+'.json')).write_text(json.dumps({'name':name,'scopeName':'source.'+id,'patterns':patterns}, indent=2)+'\n')
    (assets / (id+'-configuration.json')).write_text(json.dumps({'comments':{'lineComment': '#' if comment.startswith('#') else '--' if comment.startswith('--') else '//'}, 'brackets':[['{','}'],['[',']'],['(',')']], 'autoClosingPairs':[['{','}'],['[',']'],['(',')'],['"','"'],["'","'"]]},indent=2)+'\n')
    registry.append({'name':id,'scopeName':'source.'+id,'grammar':f'textmate/{id}.json','languageConfiguration':f'textmate/{id}-configuration.json'})
(assets/'languages.json').write_text(json.dumps({'languages':registry},indent=2)+'\n')
theme = {'name':'Scarface Dark','colors':{'editor.background':'#1E1E1E','editor.foreground':'#D4D4D4'},'tokenColors':[
    {'scope':'comment','settings':{'foreground':'#6A9955'}},
    {'scope':'keyword','settings':{'foreground':'#569CD6'}},
    {'scope':'keyword.operator','settings':{'foreground':'#D4D4D4'}},
    {'scope':'string','settings':{'foreground':'#CE9178'}},
    {'scope':'constant.numeric','settings':{'foreground':'#B5CEA8'}},
    {'scope':'entity.name.function','settings':{'foreground':'#DCDCAA'}},
    {'scope':'entity.name.tag','settings':{'foreground':'#569CD6'}},
    {'scope':'entity.other.attribute-name','settings':{'foreground':'#9CDCFE'}},
    {'scope':'variable','settings':{'foreground':'#9CDCFE'}},
    {'scope':'markup.heading','settings':{'foreground':'#00C7E6'}},
    {'scope':'markup.bold','settings':{'foreground':'#DCDCAA','fontStyle':'bold'}}]}
(assets/'dark.json').write_text(json.dumps(theme,indent=2)+'\n')
lines = ['package com.scarface.code.editor', '', 'data class LanguageDefinition(val id: String, val title: String, val extensions: List<String>, val keywords: List<String>)', 'object LanguageCatalog {', '    val languages = listOf(']
for id,name,extensions,keywords in languages:
    lines.append(f'        LanguageDefinition("{id}", "{name}", "{extensions.lower()}".split(" "), "{keywords}".split(" ").filter {{ it.isNotEmpty() }}),')
lines.extend(['    )','    fun forFilename(name: String): LanguageDefinition? {','        val ext = name.substringAfterLast(\'.\', name).lowercase()','        return languages.firstOrNull { ext in it.extensions }','    }','}'])
(root/'app/src/main/java/com/scarface/code/editor/LanguageCatalog.kt').write_text('\n'.join(lines)+'\n')
print(f'Generated {len(languages)} original language grammars')
