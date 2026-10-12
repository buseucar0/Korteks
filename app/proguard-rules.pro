# R8 (küçültme) için projeye özel kurallar.
# Şu an gerek yok: kullandığımız kütüphaneler (Compose, lifecycle) kendi kurallarını getiriyor,
# enum'ların valueOf'u (JSON okurken) varsayılan kurallarla korunuyor.
# Release derlemesinde "sınıf bulunamadı" gibi bir çökme olursa ilgili -keep kuralı buraya eklenir.
