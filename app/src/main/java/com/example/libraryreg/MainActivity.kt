package com.example.libraryreg

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF153D70)
private val Blue = Color(0xFF2478D4)
private val PaleBlue = Color(0xFFEAF4FF)
private val Green = Color(0xFF239B62)
private val Orange = Color(0xFFFFA726)
private val Ink = Color(0xFF182A40)
private val Muted = Color(0xFF65758B)

data class Member(
    val name: String,
    val idNumber: String,
    val studyProgram: String,
    val address: String,
    val phone: String,
    val email: String,
    val membership: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Navy,
                    secondary = Green,
                    background = Color(0xFFF5F8FC),
                    surface = Color.White
                )
            ) {
                LibraryRegApp(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryRegApp(context: Context) {
    val members = remember { mutableStateListOf<Member>().apply { addAll(loadMembers(context)) } }
    var page by remember { mutableStateOf("home") }
    var editing by remember { mutableStateOf<Member?>(null) }
    var detail by remember { mutableStateOf<Member?>(null) }
    var search by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Member?>(null) }
    var notice by remember { mutableStateOf("") }

    fun persist() = saveMembers(context, members)

    Scaffold(
        containerColor = Color(0xFFF5F8FC),
        topBar = {
            if (page != "home") {
                TopAppBar(
                    title = {
                        Text(
                            when (page) {
                                "form" -> if (editing == null) "Pendaftaran Anggota" else "Edit Anggota"
                                "members" -> "Daftar Anggota"
                                "detail" -> "Detail Anggota"
                                else -> "LibraryReg"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            page = if (page == "detail") "members" else "home"
                            detail = null
                            editing = null
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Navy
                    )
                )
            }
        },
        snackbarHost = {
            if (notice.isNotBlank()) {
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = { TextButton(onClick = { notice = "" }) { Text("Tutup") }
                    }
                ) { Text(notice) }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                "home" -> HomeScreen(
                    memberCount = members.size,
                    onRegister = { editing = null; page = "form" },
                    onMembers = { search = ""; page = "members" }
                )
                "form" -> MemberFormScreen(
                    initial = editing,
                    existingMembers = members,
                    onCancel = { page = if (editing == null) "home" else "members"; editing = null },
                    onSave = { member, old ->
                        val duplicate = members.any {
                            it.idNumber.trim().equals(member.idNumber.trim(), ignoreCase = true) &&
                                    it.idNumber != old?.idNumber
                        }
                        if (duplicate) {
                            notice = "NIM/NIP sudah terdaftar."
                        } else {
                            if (old != null) {
                                val index = members.indexOf(old)
                                if (index >= 0) members[index] = member
                                notice = "Data anggota berhasil diperbarui."
                                page = "members"
                            } else {
                                members.add(member)
                                notice = "Pendaftaran anggota berhasil disimpan."
                                page = "home"
                            }
                            persist()
                            editing = null
                        }
                    }
                )
                "members" -> MembersScreen(
                    members = members,
                    query = search,
                    onQueryChange = { search = it },
                    onOpen = { detail = it; page = "detail" },
                    onEdit = { editing = it; page = "form" },
                    onDelete = { deleteTarget = it }
                )
                "detail" -> detail?.let { member ->
                    DetailScreen(
                        member = member,
                        onEdit = { editing = member; page = "form" },
                        onDelete = { deleteTarget = member }
                    )
                }
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus anggota?") },
            text = { Text("Data ${deleteTarget?.name} akan dihapus dari daftar anggota.") },
            confirmButton = {
                TextButton(onClick = {
                    members.remove(deleteTarget)
                    persist()
                    if (detail == deleteTarget) {
                        detail = null
                        page = "members"
                    }
                    notice = "Data anggota berhasil dihapus."
                    deleteTarget = null
                }) { Text("Hapus", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    memberCount: Int,
    onRegister: () -> Unit,
    onMembers: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(52.dp).background(Navy, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Column {
                Text("LibraryReg", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                Text("Pendaftaran Anggota Perpustakaan", color = Muted, fontSize = 13.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Navy),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selamat datang!", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Daftarkan diri dan kelola data anggota perpustakaan dengan lebih mudah.",
                    color = Color(0xFFE1EDFF), fontSize = 14.sp, lineHeight = 21.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("$memberCount anggota terdaftar", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text("Menu Utama", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        HomeMenuCard(
            title = "Pendaftaran Anggota",
            subtitle = "Isi formulir untuk mendaftarkan anggota baru",
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Blue, modifier = Modifier.size(28.dp)) },
            onClick = onRegister
        )
        HomeMenuCard(
            title = "Daftar Anggota",
            subtitle = "Cari, lihat, ubah, atau hapus data anggota",
            icon = { Icon(Icons.Default.Groups, contentDescription = null, tint = Green, modifier = Modifier.size(28.dp)) },
            onClick = onMembers
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = PaleBlue),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Tentang LibraryReg", color = Navy, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Aplikasi untuk membantu proses pendaftaran dan pengelolaan informasi anggota perpustakaan secara digital.",
                    color = Ink, fontSize = 13.sp, lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).background(PaleBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberFormScreen(
    initial: Member?,
    existingMembers: List<Member>,
    onCancel: () -> Unit,
    onSave: (Member, Member?) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var idNumber by remember(initial) { mutableStateOf(initial?.idNumber.orEmpty()) }
    var program by remember(initial) { mutableStateOf(initial?.studyProgram.orEmpty()) }
    var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }
    var email by remember(initial) { mutableStateOf(initial?.email.orEmpty()) }
    var membership by remember(initial) { mutableStateOf(initial?.membership ?: "Mahasiswa") }
    var error by remember { mutableStateOf("") }
    var programExpanded by remember { mutableStateOf(false) }
    var membershipExpanded by remember { mutableStateOf(false) }
    val programs = listOf("Informatika", "Sistem Informasi", "Rekayasa Perangkat Lunak", "Teknik Komputer", "Lainnya")
    val memberships = listOf("Mahasiswa", "Dosen", "Tenaga Kependidikan", "Umum")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (initial == null) "Lengkapi data calon anggota" else "Perbarui informasi anggota",
            color = Ink, fontSize = 15.sp
        )
        FormField("Nama Lengkap *", name, { name = it }, "Masukkan nama lengkap")
        FormField("NIM/NIP *", idNumber, { idNumber = it }, "Masukkan nomor induk")
        ExposedDropdownMenuBox(
            expanded = programExpanded,
            onExpandedChange = { programExpanded = !programExpanded }
        ) {
            OutlinedTextField(
                value = program,
                onValueChange = { program = it },
                readOnly = true,
                label = { Text("Program Studi *") },
                placeholder = { Text("Pilih program studi") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = programExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = programExpanded, onDismissRequest = { programExpanded = false }) {
                programs.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { program = item; programExpanded = false })
                }
            }
        }
        FormField("Alamat *", address, { address = it }, "Masukkan alamat", singleLine = false)
        FormField("Nomor Telepon *", phone, { phone = it }, "Contoh: 081234567890", keyboard = KeyboardType.Phone)
        FormField("Email *", email, { email = it }, "nama@email.com", keyboard = KeyboardType.Email)
        ExposedDropdownMenuBox(
            expanded = membershipExpanded,
            onExpandedChange = { membershipExpanded = !membershipExpanded }
        ) {
            OutlinedTextField(
                value = membership,
                onValueChange = { membership = it },
                readOnly = true,
                label = { Text("Jenis Keanggotaan *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = membershipExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = membershipExpanded, onDismissRequest = { membershipExpanded = false }) {
                memberships.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { membership = item; membershipExpanded = false })
                }
            }
        }
        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFB3261E), fontSize = 13.sp)
        }
        Button(
            onClick = {
                val candidate = Member(name.trim(), idNumber.trim(), program.trim(), address.trim(), phone.trim(), email.trim(), membership)
                error = when {
                    listOf(name, idNumber, program, address, phone, email, membership).any { it.isBlank() } ->
                        "Semua kolom bertanda * wajib diisi."
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                        "Format email belum valid."
                    phone.trim().length < 8 ->
                        "Nomor telepon minimal 8 digit."
                    existingMembers.any { it.idNumber.equals(candidate.idNumber, true) && it.idNumber != initial?.idNumber } ->
                        "NIM/NIP sudah terdaftar."
                    else -> ""
                }
                if (error.isBlank()) onSave(candidate, initial)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            Text(if (initial == null) "Daftar Anggota" else "Simpan Perubahan", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Batal") }
        Text("* Wajib diisi", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
private fun MembersScreen(
    members: List<Member>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (Member) -> Unit,
    onEdit: (Member) -> Unit,
    onDelete: (Member) -> Unit
) {
    val filtered = members.filter {
        it.name.contains(query, ignoreCase = true) ||
                it.idNumber.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama atau NIM/NIP...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Text("${filtered.size} anggota ditemukan", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Muted, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(if (query.isBlank()) "Belum ada anggota terdaftar" else "Anggota tidak ditemukan", color = Muted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(filtered, key = { it.idNumber }) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).background(PaleBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.name.take(1).uppercase(), color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f).clickable { onOpen(member) }) {
                                    Text(member.name, color = Ink, fontWeight = FontWeight.Bold)
                                    Text(member.idNumber, color = Muted, fontSize = 12.sp)
                                    Text("${member.studyProgram} • ${member.membership}", color = Blue, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onOpen(member) }) { Text("Detail") }
                                IconButton(onClick = { onEdit(member) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Blue)
                                }
                                IconButton(onClick = { onDelete(member) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFD33D3D))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(member: Member, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Navy)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(76.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Text(member.name.take(1).uppercase(), color = Navy, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(member.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Text(member.idNumber, color = Color(0xFFD9E9FF))
                Spacer(Modifier.height(5.dp))
                Text(member.membership, color = Color.White)
            }
        }
        DetailRow("Program Studi", member.studyProgram)
        DetailRow("Alamat", member.address)
        DetailRow("Nomor Telepon", member.phone)
        DetailRow("Email", member.email)
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Edit Data")
        }
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD33D3D))
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Hapus Anggota")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(label, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Ink, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

private fun loadMembers(context: Context): List<Member> {
    return try {
        val raw = context.getSharedPreferences("libraryreg", Context.MODE_PRIVATE).getString("members", "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            Member(
                item.optString("name"),
                item.optString("idNumber"),
                item.optString("studyProgram"),
                item.optString("address"),
                item.optString("phone"),
                item.optString("email"),
                item.optString("membership")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveMembers(context: Context, members: List<Member>) {
    val array = JSONArray()
    members.forEach { member ->
        array.put(JSONObject().apply {
            put("name", member.name)
            put("idNumber", member.idNumber)
            put("studyProgram", member.studyProgram)
            put("address", member.address)
            put("phone", member.phone)
            put("email", member.email)
            put("membership", member.membership)
        })
    }
    context.getSharedPreferences("libraryreg", Context.MODE_PRIVATE)
        .edit().putString("members", array.toString()).apply()
}
