import pypdf
import re

reader = pypdf.PdfReader("doctors_list.pdf")
full_text = ""
for page in reader.pages:
    full_text += page.extract_text() + "\n"

lines = full_text.split("\n")
doctors = []

# List of known specializations that get merged with the name
specs_list = [
    "ﻗﻠﺐ", "ﻋﻈﺎم", "ﺟﻠﺪﻳﺔ", "ﻣﻨﺎﻇﻴﺮ", "ﺑﺎﻃﻨﻲ", "ﺑﺎﻃﻨﺔ", "ﻋﺎﻣﺔ", "وﺗﻮﻟﻴﺪ", 
    "ﺻﺪر", "ﻣﺦ", "ﻋﻴﻮن", "ﺑﻮﻟﻴﺔ", "ﻣﺴﺎﻟﻚ", "اﺳﻨﺎن", "اﻃﻔﺎل", "اورام", 
    "ﻛﻠﻰ", "ووﻻدة", "روﻣﺎﺗﻴﺰم", "اﻣﺮاض", "ﺟﺮاﺣﺔ"
]

for line in lines:
    line = line.strip()
    if not line: continue
    
    # Match pattern: (number)(name_spec)(A|B|C)(locations)
    match = re.search(r"^(\d+)\s*([^ABCabc]+)\s*([ABCabc])\s*(.*)$", line)
    if match:
        num = match.group(1)
        name_spec = match.group(2).strip()
        cls = match.group(3).upper()
        location = match.group(4).strip()
        
        # Detach merged specializations
        for spec_kw in specs_list:
            # If name_spec ends with spec_kw or contains spec_kw followed by space
            # e.g. "اﺣﻤﺪﻗﻠﺐ" -> "اﺣﻤﺪ ﻗﻠﺐ"
            name_spec = re.sub(r'([^\s])' + spec_kw + r'(\s|$)', r'\1 ' + spec_kw + r'\2', name_spec)
            
        parts = name_spec.split()
        if len(parts) >= 2:
            spec = parts[-1]
            if spec in ["ﺑﺎﻃﻨﻲ", "أﺧﺼﺎﺋﻲ", "ﺟﺮاﺣﺔ", "ﻧﺴﺎء", "وأﻋﺼﺎب", "وﺣﻨﺠﺮة", "ﻣﺴﺎﻟﻚ", "ﻃﺒﻴﺐ", "اﺳﻨﺎن", "اﻃﻔﺎل", "ﻋﻈﺎم", "ﺟﻠﺪﻳﺔ", "ﻋﻴﻮن", "ﻗﻠﺐ", "ﺑﺎﻃﻨﺔ", "اورام", "ﻣﻨﺎﻇﻴﺮ", "ﺻﺪر", "ووﻻدة", "اﺣﺼﺎﺋﻲ", "روﻣﺎﺗﻴﺰم", "اﻣﺮاض"]:
                if len(parts) >= 3 and parts[-2] in ["ﻗﻠﺐ", "ﻣﻨﺎﻇﻴﺮ", "ﻋﺎﻣﺔ", "وﺗﻮﻟﻴﺪ", "ﻣﺦ", "أﻧﻒ", "ﺑﻮﻟﻴﺔ", "ﻋﺎم", "ﻧﻔﺴﻴﺔ", "ﻛﻠﻰ", "ﺻﺪر", "ووﻻدة", "اﺧﺼﺎﺋﻲ", "روﻣﺎﺗﻴﺰم", "اﻣﺮاض", "اﺣﺼﺎﺋﻲ", "وﻏﺪد"]:
                    spec = parts[-2] + " " + spec
                    name_parts = parts[:-2]
                else:
                    name_parts = parts[:-1]
            else:
                spec = parts[-1]
                name_parts = parts[:-1]
        else:
            name_parts = [name_spec]
            spec = "عام"
        
        # Reverse the order of words in name to fix visual order extraction
        # e.g. ["اﻟﺒﻴﺸﻲ", "ﻣﺜﻨﻰ", "اﺣﻤﺪ"] -> "اﺣﻤﺪ ﻣﺜﻨﻰ اﻟﺒﻴﺸﻲ"
        name = " ".join(reversed(name_parts))
        
        # Reverse the order of words in location as well
        # e.g. "اﻻﻟﻤﺎﻧﻲ ﻋﺪن -ﻣﺴﺘﺸﻔﻰ" -> "ﻣﺴﺘﺸﻔﻰ ﻋﺪن اﻻﻟﻤﺎﻧﻲ"
        loc_parts = re.split(r'[,،]', location)
        cleaned_locs = []
        for lp in loc_parts:
            lp_words = [w.replace('-', '').strip() for w in lp.split() if w.replace('-', '').strip()]
            cleaned_locs.append(" ".join(reversed(lp_words)))
        
        location_final = " ، ".join(cleaned_locs)
        if not location_final or location_final == " ، " or location_final.isspace():
            location_final = "مستشفى عدن العام"
            
        doctors.append((name, spec, cls, location_final))

print(f"Parsed {len(doctors)} doctors.")
for d in doctors[:15]:
    print(d)

with open("app/src/main/java/com/waleed/crm/data/SeedData.kt", "w", encoding="utf-8") as f:
    f.write("package com.waleed.crm.data\n\n")
    f.write("object SeedData {\n")
    f.write("    val doctors = listOf(\n")
    for i, (name, spec, cls, loc) in enumerate(doctors):
        name = name.replace("\"", "\\\"").strip()
        spec = spec.replace("\"", "\\\"").strip()
        cls = cls.replace("\"", "\\\"").strip()
        loc = loc.replace("\"", "\\\"").strip()
        if not loc or loc == "،": loc = "مستشفى عدن العام"
        phone = f"77{i+1000000}"
        f.write(f'        Client(name = "{name}", phone = "{phone}", clientType = "طبيب", specialization = "{spec}", clientClass = "{cls}", location = "{loc}", isClassified = true)')
        if i < len(doctors) - 1:
            f.write(",\n")
        else:
            f.write("\n")
    f.write("    )\n")
    f.write("}\n")
print("SeedData.kt regenerated successfully!")
